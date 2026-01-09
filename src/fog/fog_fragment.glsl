#version 330 core
out vec4 FragColor;

uniform sampler2D depthTexture;
uniform vec2 screenSize;

uniform mat4 invProjection;
uniform mat4 invView;
uniform mat4 model;
uniform mat4 invModel;

uniform vec3 cameraPos;

uniform vec4 fogColor;
uniform float density;
uniform float baseOpacity;

uniform sampler2D noiseMap;
uniform float noiseScale;
uniform float noiseStrength;

uniform float time;        // accumulated time in seconds
uniform vec3 windDir;      // normalized direction (world or local)
uniform float windSpeed;   // units per second

struct Light {
    vec3 position;    // For point lights, this is the light position.
    vec3 color;
    vec3 attenuation; // (constant, linear, quadratic)
    float distance;
};

uniform int numLights;
uniform Light lights[16];

uniform float scatteringStrength;   // overall brightness of fog lighting (start ~1.0)
uniform float anisotropyG;          // phase g: -0.2..0.7 (0 = isotropic)

uniform float stepMultiplier;

vec3 reconstructWorldPosition(float depth, vec2 uv) {
    float z = depth * 2.0 - 1.0;
    vec4 clip = vec4(uv * 2.0 - 1.0, z, 1.0);
    vec4 viewPos = invProjection * clip;
    viewPos /= viewPos.w;
    vec4 worldPos = invView * viewPos;
    return worldPos.xyz;
}

vec3 getViewRayWorld(vec2 uv) {
    vec4 clip = vec4(uv * 2.0 - 1.0, -1.0, 1.0);
    vec4 view = invProjection * clip;
    view = vec4(view.xy, -1.0, 0.0);
    return normalize((invView * view).xyz);
}

bool intersectBox(vec3 ro, vec3 rd, out float tMin, out float tMax) {
    vec3 invD = 1.0 / rd;
    vec3 t0 = (-0.5 - ro) * invD;
    vec3 t1 = ( 0.5 - ro) * invD;

    vec3 tSmaller = min(t0, t1);
    vec3 tBigger  = max(t0, t1);

    tMin = max(max(tSmaller.x, tSmaller.y), tSmaller.z);
    tMax = min(min(tBigger.x,  tBigger.y),  tBigger.z);

    return tMax >= max(tMin, 0.0);
}


float interiorFalloff(vec3 worldPos) {

    // Local cube space (-0.5 .. +0.5)
    vec3 p = (invModel * vec4(worldPos, 1.0)).xyz;

    // Signed distance to a rounded box
    // q = distance outside the inner rounded core
    vec3 q = abs(p) - vec3(0.5);

    float outside = length(max(q, 0.0));
    float inside  = min(max(q.x, max(q.y, q.z)), 0.0);

    float sdf = outside + inside; // signed distance

    // sdf <= 0 inside, 0 at surface, positive outside
    // We want: 0 at boundary → 1 deeper inside

    float t = clamp(-sdf / 0.5, 0.0, 1.0);

    // Smooth nonlinear growth inward
    return t * t * (3.0 - 2.0 * t);
}



float noise2D(vec2 uv) {
    return texture(noiseMap, uv).r;
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;

    // 4 octaves is enough for clouds
    for (int i = 0; i < 4; i++) {
        float nXY = noise2D(p.xy);
        float nXZ = noise2D(p.xz);
        float nYZ = noise2D(p.yz);

        float n = (nXY + nXZ + nYZ) / 3.0;

        value += n * amplitude;

        p *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}


float sampleNoise(vec3 worldPos) {

    vec3 local = (invModel * vec4(worldPos, 1.0)).xyz;

    vec3 p = (local + 0.5) * noiseScale;

    // Periodic wind advection
    vec3 windOffset = fract(windDir * (time * windSpeed));
    p += windOffset;

    // Domain warp (still works!)
    vec3 warp = vec3(
        noise2D(p.yz),
        noise2D(p.xz),
        noise2D(p.xy)
    );

    p += warp * 0.35;

    return fbm(p);
}


float phaseHG(float cosTheta, float g) {
    g = clamp(g, -0.95, 0.95);
    float g2 = g*g;
    float denom = 1.0 + g2 - 2.0*g*cosTheta;
    denom = max(denom, 1e-4);

    float invSqrt = inversesqrt(denom);
    float invDenomPow = invSqrt / denom;

    return (1.0 - g2) * (1.0 / (4.0 * 3.14159265)) * invDenomPow;
}


// Same box test but for a point: inside the cube in LOCAL coords
bool insideVolumeLocal(vec3 pLocal) {
    return all(lessThanEqual(abs(pLocal), vec3(0.5)));
}

float computeLightTransmittance(vec3 pWorld, vec3 lightPosWorld) {

    vec3 toL = lightPosWorld - pWorld;
    float dist = length(toL);
    if (dist <= 1e-4) return 1.0;

    vec3 dir = toL / dist;

    // Small number of segments for speed
    const int LSTEPS = 8;
    float ds = dist / float(LSTEPS);

    float tau = 0.0;

    // march from pWorld toward the light
    for (int i = 0; i < LSTEPS; i++) {
        float t = (float(i) + 0.5) * ds;
        vec3 sWorld = pWorld + dir * t;

        // Only integrate where the sample point is INSIDE the fog volume
        vec3 sLocal = (invModel * vec4(sWorld, 1.0)).xyz;
        if (!insideVolumeLocal(sLocal)) continue;

        // Reuse your existing density field (interior + noise)
        float interior = interiorFalloff(sWorld);

        // IMPORTANT: don't use view-based volumeDepth here (light ray is different)
        // Instead, rely on interior shaping + noise to define fog density
        float base = density * interior;

        float n = sampleNoise(sWorld);
        float cloud = smoothstep(0.4, 0.75, n);
        cloud *= n;

        float sigmaT = base * mix(0.2, 1.8, cloud) * noiseStrength;
        sigmaT = max(sigmaT, 0.0);

        tau += sigmaT * ds;
        if (tau >= 8.0) break;
    }

    return exp(-tau);
}




void main() {

    vec2 uv = gl_FragCoord.xy / screenSize;

    // --- World ray ---
    vec3 rayDirWorld = getViewRayWorld(uv);

    // --- Scene depth distance ALONG THE RAY (not length) ✅ ---
    float sceneDepth = texture(depthTexture, uv).r;
    float sceneT = 1e20; // "infinite"

    if (sceneDepth < 1.0) {
        vec3 scenePosWorld = reconstructWorldPosition(sceneDepth, uv);
        sceneT = dot(scenePosWorld - cameraPos, rayDirWorld);  // ✅
        // If something goes weird, keep it sane:
        sceneT = max(sceneT, 0.0);
    }

   // --- Convert ray into local cube space for intersection ---
	vec3 roLocal = (invModel * vec4(cameraPos, 1.0)).xyz;
	vec3 rdLocal = normalize((invModel * vec4(rayDirWorld, 0.0)).xyz);
	
	float tEnterL, tExitL;
	if (!intersectBox(roLocal, rdLocal, tEnterL, tExitL)) discard;
	tEnterL = max(tEnterL, 0.0);
	
	// --- Compute entry/exit in WORLD, then convert to WORLD ray t ✅ ---
	vec3 pEnterWorld = (model * vec4(roLocal + rdLocal * tEnterL, 1.0)).xyz;
	vec3 pExitWorld  = (model * vec4(roLocal + rdLocal * tExitL , 1.0)).xyz;
	
	float tEnterW = dot(pEnterWorld - cameraPos, rayDirWorld);
	float tExitW  = dot(pExitWorld  - cameraPos, rayDirWorld);
	
	// Guard against weirdness
	if (tExitW <= tEnterW) discard;
	tEnterW = max(tEnterW, 0.0);
	
	// --- Ray march in WORLD t ✅ ---
	float volumeLength = tExitW - tEnterW;

	// Fewer steps for thin volumes
	float baseSteps = clamp(volumeLength * 8.0, 16.0, 48.0);

	// Scale with uniform
	int STEPS = int(clamp(baseSteps * stepMultiplier, 8.0, 128.0));
	
	float stepW = volumeLength / float(STEPS);
			
	float tW = tEnterW;
	
	// Track view transmittance directly for better numerical stability
	float Tview = 1.0;
	
	vec3 inscatter = vec3(0.0);
	
	for (int i = 0; i < STEPS; i++) {
	
	    if (tW > sceneT) break;
	
	    vec3 pWorld = cameraPos + rayDirWorld * tW;
	
	   
	
	    float interior = interiorFalloff(pWorld);
	
	   float base = density * interior;
	
	    float n = sampleNoise(pWorld);
	    float cloud = smoothstep(0.4, 0.75, n);
	    cloud *= n;
	
	    float sigmaT = base * mix(0.2, 1.8, cloud) * noiseStrength;
	    sigmaT = max(sigmaT, 0.0);
	    
	    if (sigmaT < 0.0005) {
		    tW += stepW;
		    continue;
		}
			
			    // Extinction along view ray
		float segmentTau = sigmaT * stepW;
		float segmentTr  = exp(-segmentTau);
		
		// If you don't separate absorption vs scattering,
		// assume everything is scattering:
		float sigmaS = sigmaT;
		
		// Single-scatter contribution factor for this segment
		// (integral over segment): approx sigmaS * ds * Tview
		float scatterWeight = sigmaS * stepW;
		
		// --- In-scattering from lights ---
		for (int l = 0; l < numLights; l++) {
		
	
		
		    vec3 Lvec = lights[l].position - pWorld;
		    float d = length(Lvec);
		    if (d <= 1e-4) continue;
		    
		    float lightRange = lights[l].distance;
			if (lightRange > 0.0 && d > lightRange) continue;
			
		    vec3 Ldir = Lvec / d;
		
		    float att = 1.0 / (lights[l].attenuation.x
		                     + lights[l].attenuation.y * d
		                     + lights[l].attenuation.z * d * d);
		
		    if (lights[l].distance > 0.0) {
		        float range = clamp(1.0 - (d / lights[l].distance), 0.0, 1.0);
		        att *= range * range;
		    }
		
		    // HG phase: uses angle between light direction and view direction
		    float cosTheta = dot(Ldir, -rayDirWorld);
		    float phase = phaseHG(cosTheta, anisotropyG);
		
		    //float Tlight = computeLightTransmittance(pWorld, lights[l].position);
		    
		    float approxLightDist = d; // distance to light
			float avgDensity = sigmaT; // local density proxy
			float Tlight = exp(-avgDensity * approxLightDist);
		
		    vec3 Li = lights[l].color * att * Tlight;
		
		    // ✅ HG single scattering
		    inscatter += (Tview * scatterWeight) * Li * phase;
		}
		
		// Update view transmittance after segment
		Tview *= segmentTr;

	
	    // Early out when nearly opaque
	    if (Tview < 0.01) break;
	
	    tW += stepW;
	}
	

	float alpha = 1.0 - Tview;
	alpha *= baseOpacity;
	if (alpha <= 0.001) discard;
	
	// Base fog color (albedo-ish) plus in-scattered light
	vec3 fogBase = fogColor.rgb;
	
	// scatteringStrength lets you tune brightness without wrecking density
	vec3 litFog = fogBase * alpha + inscatter * scatteringStrength;
	
	// Clamp for safety (HDR pipeline can remove this)
	litFog = clamp(litFog, 0.0, 50.0);
	
	FragColor = vec4(litFog, alpha);

}
