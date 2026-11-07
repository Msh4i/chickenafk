package dev.chickenafk.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * A posable player model for GUI use. It is the vanilla player mesh with each leg split at the knee
 * (6px thigh + 8px shin, overlapping 2px) so knees can bend; standing, it looks identical to vanilla.
 * Every figure owns its model, so figures drawn in the same frame can hold different poses.
 */
public final class Figure {
	private static final float DEG = Mth.DEG_TO_RAD;
	private static final CubeDeformation OUTER = new CubeDeformation(0.25f);
	/** Screenshot runs outline each figure's render box, so clipping is easy to spot. */
	private static final boolean SHOW_BOXES = Boolean.getBoolean("chickenafk.shots") || System.getProperty("chickenafk.lab") != null;

	private final PlayerModel model;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final float[] cur = new float[Pose.SIZE];
	private Pose pose;
	private long lastNanos = System.nanoTime();
	private float time;

	public Figure(boolean slim, Pose pose) {
		ModelPart root = LayerDefinition.create(kneeMesh(slim), 64, 64).bakeRoot();
		model = new PlayerModel(root, slim);
		rightShin = model.rightLeg.getChild("right_shin");
		leftShin = model.leftLeg.getChild("left_shin");
		this.pose = pose;
		System.arraycopy(pose.values, 0, cur, 0, Pose.SIZE);
	}

	/** Change pose; the figure eases into it instead of snapping. */
	public void setPose(Pose pose) {
		this.pose = pose;
	}

	public Pose pose() {
		return pose;
	}

	/** Renders into a GUI-space box. rotationY turns the whole figure, in degrees. */
	public void render(GuiGraphics g, Identifier skin, int x0, int y0, int x1, int y1, float rotationY) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
		lastNanos = now;
		time += dt;
		float ease = 1 - (float) Math.exp(-dt * 3.2f);
		for (int i = 0; i < Pose.SIZE; i++) {
			cur[i] += (pose.values[i] - cur[i]) * ease;
		}
		apply();
		submit(g, skin, x0, y0, x1, y1, -6f + cur[Pose.PITCH], rotationY + cur[Pose.TURN]);
	}

	/** Fixed camera, for the dev pose lab: no pose-specific pitch or turn added. */
	public void renderView(GuiGraphics g, Identifier skin, int x0, int y0, int x1, int y1, float rotationX, float rotationY) {
		System.arraycopy(pose.values, 0, cur, 0, Pose.SIZE);
		apply();
		submit(g, skin, x0, y0, x1, y1, rotationX, rotationY);
	}

	/**
	 * The given box is the standing body. The skin is drawn into a box grown sideways and upward, so
	 * raised or outstretched limbs are not cut off; the scale still comes from the body height. The
	 * bottom stays put because vanilla anchors the model to the bottom edge of the box.
	 */
	private void submit(GuiGraphics g, Identifier skin, int x0, int y0, int x1, int y1, float rotationX, float rotationY) {
		int h = y1 - y0;
		float scale = 0.97f * h / 2.125f;
		int cx = (x0 + x1) / 2;
		int halfW = Math.round(h * 0.62f);
		int bx0 = cx - halfW;
		int by0 = y0 - Math.round(h * 0.3f);
		int bx1 = cx + halfW;
		g.submitSkinRenderState(model, skin, scale, rotationX, rotationY, -1.0625f, bx0, by0, bx1, y1);
		if (SHOW_BOXES) {
			// box coords are GUI units; callers may be drawing under a scaled pose
			g.pose().pushMatrix();
			g.pose().identity();
			g.renderOutline(bx0, by0, bx1 - bx0, y1 - by0, 0xFFFF4040);
			g.pose().popMatrix();
		}
	}

	private void apply() {
		model.resetPose();
		float drop = cur[Pose.DROP];
		for (ModelPart p : new ModelPart[] {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg}) {
			p.y += drop;
		}
		set(model.head, Pose.HEAD);
		set(model.rightArm, Pose.R_ARM);
		set(model.leftArm, Pose.L_ARM);
		set(model.rightLeg, Pose.R_LEG);
		set(model.leftLeg, Pose.L_LEG);
		set(rightShin, Pose.R_SHIN);
		set(leftShin, Pose.L_SHIN);
		rightShin.y += cur[Pose.R_SHIN_POS];
		rightShin.z += cur[Pose.R_SHIN_POS + 1];
		leftShin.y += cur[Pose.L_SHIN_POS];
		leftShin.z += cur[Pose.L_SHIN_POS + 1];

		// small looping motion on top of the held pose
		float s = Mth.sin(time * 5.7f);
		switch (pose.motion) {
			case IDLE -> {
				float sway = Mth.sin(time * 1.9f) * 4 * DEG;
				model.rightArm.xRot += sway;
				model.leftArm.xRot -= sway;
				model.rightArm.zRot += 3 * DEG;
				model.leftArm.zRot -= 3 * DEG;
			}
			case WAVE -> model.rightArm.zRot += s * 11 * DEG;
			case CHEER -> {
				model.rightArm.zRot += s * 11 * DEG;
				model.leftArm.zRot += s * 11 * DEG;
			}
			case NOD -> model.head.xRot += (Mth.sin(time * 2.4f) + 1) * 3 * DEG;
			case LOOK -> model.head.yRot += Mth.sin(time * 0.9f) * 12 * DEG;
			case BREATHE -> {
				float b = Mth.sin(time * 1.75f) * 0.25f;
				model.head.y += b;
				model.body.y += b;
				model.rightArm.y += b;
				model.leftArm.y += b;
				model.head.yRot += Mth.sin(time * 0.9f) * 14 * DEG;
			}
			case NONE -> {
			}
		}
	}

	private void set(ModelPart part, int at) {
		part.xRot = cur[at] * DEG;
		part.yRot = cur[at + 1] * DEG;
		part.zRot = cur[at + 2] * DEG;
	}

	/** Vanilla player mesh with each leg replaced by thigh + shin (pants follow both). */
	private static MeshDefinition kneeMesh(boolean slim) {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
		PartDefinition root = mesh.getRoot();
		leg(root, "right", -1.9f, 0, 16, 0, 32);
		leg(root, "left", 1.9f, 16, 48, 0, 48);
		return mesh;
	}

	private static void leg(PartDefinition root, String side, float x, int u, int v, int pu, int pv) {
		// thigh shows texture rows v+4..v+10, the shin rows v+8..v+16 (starting 2px inside the thigh)
		PartDefinition thigh = root.addOrReplaceChild(side + "_leg",
				CubeListBuilder.create().texOffs(u, v).addBox(-2, 0, -2, 4, 6, 4, CubeDeformation.NONE), PartPose.offset(x, 12, 0));
		thigh.addOrReplaceChild(side + "_pants",
				CubeListBuilder.create().texOffs(pu, pv).addBox(-2, 0, -2, 4, 6, 4, OUTER), PartPose.offset(0, 0, 0));
		PartDefinition shin = thigh.addOrReplaceChild(side + "_shin",
				CubeListBuilder.create().texOffs(u, v + 4).addBox(-2, -2, -2, 4, 8, 4, CubeDeformation.NONE), PartPose.offset(0, 6, 0));
		shin.addOrReplaceChild(side + "_shin_pants",
				CubeListBuilder.create().texOffs(pu, pv + 4).addBox(-2, -2, -2, 4, 8, 4, OUTER), PartPose.offset(0, 0, 0));
	}
}
