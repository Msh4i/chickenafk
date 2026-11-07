package dev.chickenafk.ui;

import java.util.List;
import java.util.Random;

/**
 * A held pose in degrees, in model terms: xRot negative swings a limb forward, zRot positive moves the
 * right arm/leg outward (negative for the left), head xRot positive looks down. DROP lowers the upper
 * body for sitting; TURN adds to the figure's facing.
 */
public final class Pose {
	static final int HEAD = 0;
	static final int R_ARM = 3;
	static final int L_ARM = 6;
	static final int R_LEG = 9;
	static final int L_LEG = 12;
	static final int R_SHIN = 15;
	static final int L_SHIN = 18;
	static final int DROP = 21;
	static final int TURN = 22;
	/** Camera tilt added to the figure's default; sitting reads better seen from a bit above. */
	static final int PITCH = 23;
	/** Shin pivot shift inside the thigh (along the thigh, then up/down), to stack crossed shins. */
	static final int R_SHIN_POS = 24;
	static final int L_SHIN_POS = 26;
	static final int SIZE = 28;

	enum Motion { NONE, IDLE, WAVE, CHEER, NOD, LOOK, BREATHE }

	public final String name;
	/** Which of the maker's expression textures (1 normal, 4 surprised, 5 happy, 6 shy, 7 sleepy) goes with this pose. */
	public final int face;
	final Motion motion;
	final float[] values = new float[SIZE];

	private Pose(String name, int face, Motion motion) {
		this.name = name;
		this.face = face;
		this.motion = motion;
	}

	private Pose part(int at, float x, float y, float z) {
		values[at] = x;
		values[at + 1] = y;
		values[at + 2] = z;
		return this;
	}

	private Pose drop(float d) {
		values[DROP] = d;
		return this;
	}

	private Pose turn(float t) {
		values[TURN] = t;
		return this;
	}

	private Pose pitch(float p) {
		values[PITCH] = p;
		return this;
	}

	private Pose shinAt(int at, float along, float down) {
		values[at] = along;
		values[at + 1] = down;
		return this;
	}

	public static final Pose IDLE = new Pose("idle", 1, Motion.IDLE);

	public static final Pose WAVE = new Pose("wave", 5, Motion.WAVE)
			.part(R_ARM, 0, 0, 140).part(L_ARM, -8, 0, -6).part(HEAD, 0, 0, 4)
			.part(R_LEG, -8, 0, 0).part(L_LEG, 6, 0, 0);

	public static final Pose HIPS = new Pose("hips", 1, Motion.NOD)
			.part(R_ARM, 8, 0, 30).part(L_ARM, 8, 0, -30).part(HEAD, 0, 10, -7)
			.part(R_LEG, 0, 0, 5).part(L_LEG, 0, 0, -5);

	public static final Pose THINK = new Pose("think", 4, Motion.NONE)
			.part(R_ARM, -118, 0, -22).part(L_ARM, -28, 0, 14).part(HEAD, 6, 16, 8)
			.part(L_LEG, 4, 0, 0).turn(-6);

	public static final Pose CHEER = new Pose("cheer", 5, Motion.CHEER)
			.part(R_ARM, 0, 0, 150).part(L_ARM, 0, 0, -150).part(HEAD, -12, 0, 0)
			.part(R_LEG, 0, 0, 3).part(L_LEG, 0, 0, -3);

	public static final Pose SHY = new Pose("shy", 6, Motion.NONE)
			.part(R_ARM, -24, 0, -16).part(L_ARM, -24, 0, 16).part(HEAD, 14, -12, 0)
			.part(R_LEG, 0, 0, -3).part(L_LEG, 6, 0, 5);

	/**
	 * Cross-legged. Thighs point forward and 35° out; a shin turned only 90° would follow that outward
	 * angle, so it turns 90+35° to lie straight across. The right shin sits in front of and a little above
	 * the left one. Arms reach from the shoulders down to the knees (about 31° forward).
	 */
	public static final Pose SIT = new Pose("sit", 7, Motion.BREATHE)
			.drop(8).turn(-6).part(HEAD, 6, 0, 0)
			.part(R_LEG, -92, 35, 0).part(L_LEG, -92, -35, 0)
			.part(R_SHIN, 0, 0, -125).shinAt(R_SHIN_POS, 0, -0.7f)
			.part(L_SHIN, 0, 0, 125).shinAt(L_SHIN_POS, -1.6f, 0.4f)
			.part(R_ARM, -34, 0, 4).part(L_ARM, -34, 0, -4);

	public static final List<Pose> MAKER = List.of(WAVE, HIPS, THINK, CHEER, SHY, SIT);

	/** Tour guide pointing to the viewer's right, with the character's left arm. */
	public static final Pose POINT_RIGHT = new Pose("point-right", 5, Motion.NONE)
			.part(L_ARM, -4, 0, -88).part(R_ARM, -6, 0, 4).part(HEAD, 0, -22, 0);

	/** Tour guide pointing to the viewer's left, with the character's right arm. */
	public static final Pose POINT_LEFT = new Pose("point-left", 5, Motion.NONE)
			.part(R_ARM, -4, 0, 88).part(L_ARM, -6, 0, -4).part(HEAD, 0, 22, 0);

	public static Pose randomMaker(Random random, Pose not) {
		Pose p;
		do {
			p = MAKER.get(random.nextInt(MAKER.size()));
		} while (p == not);
		return p;
	}

	public static Pose byName(String name) {
		for (Pose p : MAKER) {
			if (p.name.equals(name)) {
				return p;
			}
		}
		return IDLE;
	}
}
