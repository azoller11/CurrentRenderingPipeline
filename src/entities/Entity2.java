package entities;

import java.util.List;

import org.joml.Vector3f;

import com.bulletphysics.dynamics.RigidBody;

import animatedModel.AnimatedModel;
import animatedModel.AnimationElement;
import animation.Animator;
import toolbox.Material;
import toolbox.Mesh;

public class Entity2 {
	
	private int Id;
	private Vector3f position;
    private Vector3f rotation; // rotation.x => pitch, rotation.y => yaw, rotation.z => roll
    private float scale;
    
    private Material material;
    private Mesh mesh;
    
	private AnimatedModel animatedModel;
	private List<AnimatedModel> animatedModels;
    
    
    private int activeAnimation = -1;
    private List<AnimationElement> animationState;
    private Animator animator;
    
    private RigidBody collisionBody;
    private String collisionType = "STATIC";
    private float mass = 0f;
    
    
    private boolean castShadows;
    
    private boolean isVegitation;
    private boolean useFakeLighting;
    
    
    
	

}
