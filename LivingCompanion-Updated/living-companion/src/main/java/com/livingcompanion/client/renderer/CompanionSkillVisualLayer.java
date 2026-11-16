package com.livingcompanion.client.renderer;

import com.livingcompanion.entity.CompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side MMORPG-inspired geometry for companion skills.
 *
 * All skill decisions remain server-authoritative. The renderer consumes
 * synchronized visual flags and creates no entities or gameplay effects.
 */
public final class CompanionSkillVisualLayer
        extends RenderLayer<CompanionEntity, CompanionKnightModel> {

    private static final int DIVINE_PROTECTION = 1;
    private static final int SHIELD_WALL = 1 << 1;
    private static final int PRAYER = 1 << 2;
    private static final int AEGIS = 1 << 3;
    private static final int LAST_BASTION = 1 << 4;
    private static final int TAUNT = 1 << 5;
    private static final int INTERCEPT = 1 << 6;
    private static final int HOLY_STRIKE = 1 << 7;
    private static final int UNYIELDING = 1 << 8;
    private static final int DIVINE_GRACE = 1 << 9;

    public CompanionSkillVisualLayer(
            RenderLayerParent<CompanionEntity, CompanionKnightModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       CompanionEntity companion, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        int flags = companion.getSkillVisualFlags();
        if (flags == 0) return;

        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        float time = ageInTicks;

        if ((flags & SHIELD_WALL) != 0) renderOrbitingShields(poseStack, out, time, 6, 0.92F, 42, 145, 255);
        if ((flags & LAST_BASTION) != 0) {
            renderRing(poseStack, out, 0.08F, 1.48F, 0.035F, 64, 255, 190, 65, 205, time * -0.035F);
            renderOrbitingShields(poseStack, out, time, 8, 1.22F, 255, 190, 65);
            renderVerticalAura(poseStack, out, time, 1.28F, 0.055F, 255, 205, 95, 105);
            renderHalo(poseStack, out, time, 1.72F, 0.48F, 255, 225, 145);
        }
        if ((flags & DIVINE_PROTECTION) != 0) {
            renderPaladinBarrier(poseStack, out, time, 0.94F, 255, 205, 85);
            renderRing(poseStack, out, 0.09F, 1.02F, 0.065F, 64, 255, 205, 85, 190, time * 0.025F);
            renderHalo(poseStack, out, time, 1.65F, 0.34F, 255, 235, 170);
        }
        if ((flags & AEGIS) != 0) {
            renderAegisWings(poseStack, out, time);
            renderPaladinBarrier(poseStack, out, time, 1.28F, 255, 225, 130);
            renderHalo(poseStack, out, time, 2.02F, 0.58F, 255, 245, 185);
            renderRing(poseStack, out, 0.07F, 1.42F, 0.075F, 72, 255, 205, 95, 210, time * -0.045F);
            renderRing(poseStack, out, 1.32F, 0.74F, 0.035F, 56, 255, 240, 175, 145, time * 0.035F);
        }
        if ((flags & PRAYER) != 0) {
            renderPrayerCircle(poseStack, out, time);
            renderHealingRibbons(poseStack, out, time);
            renderHalo(poseStack, out, time, 1.95F, 0.28F, 255, 245, 195);
        }
        if ((flags & TAUNT) != 0) {
            // A broad, readable ground sigil replaces the thin debug-like rings.
            renderTauntSigil(poseStack, out, time);
        }
        if ((flags & INTERCEPT) != 0) {
            renderInterceptTether(poseStack, out, companion, time);
            renderRing(poseStack, out, 0.92F, 0.72F, 0.045F, 40, 90, 190, 255, 230, time * 0.12F);
        }
        if ((flags & HOLY_STRIKE) != 0) {
            renderHolySlash(poseStack, out, time);
        }
        if ((flags & UNYIELDING) != 0) {
            renderRing(poseStack, out, 0.72F, 0.55F, 0.018F, 40, 255, 125, 55, 95, time * 0.025F);
            renderRing(poseStack, out, 1.12F, 0.48F, 0.014F, 40, 255, 190, 80, 75, time * -0.03F);
        }
        if ((flags & DIVINE_GRACE) != 0) {
            renderHalo(poseStack, out, time, 1.94F, 0.30F, 255, 240, 175);
            renderRing(poseStack, out, 1.34F, 0.62F, 0.035F, 48, 255, 220, 135, 125, time * 0.018F);
            renderGraceAura(poseStack, out, time);
        }
    }

    

    private static void renderPaladinBarrier(PoseStack pose, VertexConsumer out, float time,
                                             float radius, int red, int green, int blue) {
        Matrix4f m=pose.last().pose();
        for(int i=0;i<8;i++) {
            double a=time*0.012D+Math.PI*2.0D*i/8.0D;
            float cx=(float)Math.cos(a), cz=(float)Math.sin(a);
            float tx=-cz*0.30F, tz=cx*0.30F;
            float x=cx*radius, z=cz*radius;
            int alpha=54+(int)((Math.sin(time*0.12F+i)+1.0F)*18.0F);
            quad(out,m,x-tx,0.38F,z-tz,x-tx*0.72F,1.76F,z-tz*0.72F,
                    x+tx*0.72F,1.76F,z+tz*0.72F,x+tx,0.38F,z+tz,
                    red,green,blue,alpha);
            bar3D(out,m,x-tx,0.38F,z-tz,x-tx*0.72F,1.76F,z-tz*0.72F,
                    0.025F,255,245,190,210);
            bar3D(out,m,x+tx,0.38F,z+tz,x+tx*0.72F,1.76F,z+tz*0.72F,
                    0.025F,255,245,190,210);
        }
    }

    private static void renderPrayerCircle(PoseStack pose, VertexConsumer out, float time) {
        Matrix4f m=pose.last().pose();
        float rotation=time*0.018F;
        renderDisc(pose,out,0.035F,1.16F,64,255,202,86,34);
        renderRing(pose,out,0.052F,1.18F,0.095F,72,255,211,105,225,rotation);
        renderRing(pose,out,0.060F,0.91F,0.035F,64,255,245,185,235,-rotation*1.5F);
        renderRing(pose,out,0.065F,0.55F,0.055F,56,240,165,55,190,rotation*2.0F);
        for(int i=0;i<8;i++) {
            double a=rotation+Math.PI*2.0D*i/8.0D;
            float x=(float)Math.cos(a), z=(float)Math.sin(a);
            bar3D(out,m,x*0.62F,0.072F,z*0.62F,x*1.02F,0.072F,z*1.02F,
                    0.035F,255,235,155,230);
        }
    }

    private static void renderHealingRibbons(PoseStack pose, VertexConsumer out, float time) {
        Matrix4f m=pose.last().pose();
        for(int i=0;i<6;i++) {
            double a=time*0.035D+i*Math.PI/3.0D;
            float x=(float)Math.cos(a), z=(float)Math.sin(a);
            float y=0.18F+(float)((time*0.018F+i*0.17F)%1.0F)*1.42F;
            float w=0.075F;
            quad(out,m,x*0.42F-w,y,z*0.42F,x*0.70F-w,y+0.28F,z*0.70F,
                    x*0.70F+w,y+0.28F,z*0.70F,x*0.42F+w,y,z*0.42F,
                    255,235,155,105);
        }
    }

    private static void renderGraceAura(PoseStack pose, VertexConsumer out, float time) {
        Matrix4f m=pose.last().pose();
        for(int i=0;i<12;i++) {
            double a=time*0.012D+i*Math.PI/6.0D;
            float x=(float)Math.cos(a), z=(float)Math.sin(a);
            float y=0.38F+(float)Math.sin(time*0.045F+i)*0.18F;
            quad(out,m,x*0.48F,y,z*0.48F,x*0.70F,y+0.28F,z*0.70F,
                    x*0.73F,y+0.31F,z*0.73F,x*0.50F,y+0.03F,z*0.50F,
                    255,220,125,55);
        }
    }

    private static void renderAegisWings(PoseStack pose, VertexConsumer out, float time) {
        float flap=(float)Math.sin(time*0.085F)*0.16F;
        Matrix4f m=pose.last().pose();
        for(int side=-1;side<=1;side+=2) {
            float s=side;
            quad(out,m,0.10F*s,1.35F,0.10F,0.48F*s,1.95F+flap,0.16F,
                    1.12F*s,1.70F+flap,0.22F,0.72F*s,1.18F,0.16F,
                    255,226,145,78);
            for(int f=0;f<5;f++) {
                float rx=(0.28F+f*0.075F)*s, tx=(0.52F+f*0.13F)*s;
                float ry=1.48F-f*0.055F, ty=1.86F-f*0.13F+flap;
                quad(out,m,rx,ry,0.13F,tx,ty,0.18F,(tx+rx)*0.5F,ty-0.19F,0.20F,
                        rx+0.06F*s,ry-0.15F,0.16F,255,245,190,105);
            }
        }
    }

    /**
     * Layered Tank taunt rune: filled segmented ground seal, heavy outer bands,
     * radial glyphs and a raised expanding shockwave. Geometry is camera-visible
     * from above and remains anchored to the companion's moving model origin.
     */
    private static void renderTauntSigil(PoseStack pose, VertexConsumer out, float time) {
        Matrix4f m = pose.last().pose();
        float pulse = 0.88F + (float)Math.sin(time * 0.18F) * 0.12F;
        float rotation = -time * 0.025F;

        // Broad, low-opacity central seal.
        renderDisc(pose, out, 0.035F, 0.72F, 48, 255, 72, 28, 42);
        renderRing(pose, out, 0.052F, 1.55F, 0.105F, 72,
                255, 70, 28, (int)(205 * pulse), rotation);
        renderRing(pose, out, 0.060F, 1.34F, 0.035F, 64,
                255, 175, 70, 230, -rotation * 1.4F);
        renderRing(pose, out, 0.064F, 0.78F, 0.065F, 56,
                220, 38, 20, 190, rotation * 1.8F);
        renderRing(pose, out, 0.070F, 0.48F, 0.025F, 48,
                255, 205, 105, 235, -rotation * 2.0F);

        // Segmented radial spokes and angular rune marks.
        for (int i = 0; i < 12; i++) {
            double a = rotation + Math.PI * 2.0D * i / 12.0D;
            float c = (float)Math.cos(a), s = (float)Math.sin(a);
            float inner = 0.86F, outer = (i % 3 == 0) ? 1.27F : 1.12F;
            bar3D(out, m, c * inner, 0.073F, s * inner,
                    c * outer, 0.073F, s * outer, 0.035F,
                    255, 145, 48, 220);
            float tx = c * 1.43F, tz = s * 1.43F;
            float px = -s * 0.085F, pz = c * 0.085F;
            quad(out, m, tx-px,0.076F,tz-pz, tx+c*0.075F,0.076F,tz+s*0.075F,
                    tx+px,0.076F,tz+pz, tx-c*0.075F,0.076F,tz-s*0.075F,
                    255,205,105,235);
        }

        // Three rotating triangular seals give the circle a deliberate rune design.
        for (int layer = 0; layer < 3; layer++) {
            float radius = 0.96F + layer * 0.12F;
            double offset = rotation * (layer % 2 == 0 ? 1.0D : -1.0D)
                    + layer * Math.PI / 3.0D;
            for (int i = 0; i < 3; i++) {
                double a = offset + Math.PI * 2.0D * i / 3.0D;
                double b = offset + Math.PI * 2.0D * (i + 1) / 3.0D;
                bar3D(out, m, (float)Math.cos(a)*radius,0.078F,(float)Math.sin(a)*radius,
                        (float)Math.cos(b)*radius,0.078F,(float)Math.sin(b)*radius,
                        0.018F,255,105 + layer*25,45,180);
            }
        }

        // A broad shockwave travels outward and fades over its short cycle.
        float wave = (time * 0.045F) % 1.0F;
        float waveRadius = 0.55F + wave * 1.30F;
        int waveAlpha = (int)(185 * (1.0F - wave));
        renderRing(pose, out, 0.105F, waveRadius, 0.085F, 64,
                255, 105, 38, waveAlpha, -time * 0.018F);

        // Short energy pillars rise from the outer runes, with varied heights.
        for (int i = 0; i < 8; i++) {
            double a = rotation + Math.PI * 2.0D * i / 8.0D;
            float x = (float)Math.cos(a) * 1.18F;
            float z = (float)Math.sin(a) * 1.18F;
            float height = 0.48F + (float)Math.sin(time*0.14F+i*1.7F)*0.16F;
            float width = 0.035F;
            quad(out,m,x-width,0.09F,z,x-width,height,z,x+width,height,z,x+width,0.09F,z,
                    255,105,35,135);
            quad(out,m,x,0.09F,z-width,x,height,z-width,x,height,z+width,x,0.09F,z+width,
                    255,195,90,105);
        }
    }

    private static void renderDisc(PoseStack pose, VertexConsumer out, float y, float radius,
                                   int segments, int red, int green, int blue, int alpha) {
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            quad(out, m, 0,y,0,
                    (float)Math.cos(a)*radius,y,(float)Math.sin(a)*radius,
                    (float)Math.cos(b)*radius,y,(float)Math.sin(b)*radius,
                    0,y,0, red, green, blue, alpha);
        }
    }

    private static void bar3D(VertexConsumer out, Matrix4f m,
                              float x1,float y1,float z1,float x2,float y2,float z2,
                              float width,int r,int g,int b,int a) {
        float dx=x2-x1, dz=z2-z1;
        float length=(float)Math.sqrt(dx*dx+dz*dz);
        if(length<0.0001F) return;
        float ox=-dz/length*width, oz=dx/length*width;
        quad(out,m,x1+ox,y1,z1+oz,x2+ox,y2,z2+oz,
                x2-ox,y2,z2-oz,x1-ox,y1,z1-oz,r,g,b,a);
    }

    private static void renderOrbitingShields(PoseStack pose, VertexConsumer out, float time,
                                               int count, float radius, int red, int green, int blue) {
        float orbit = time * 0.035F;
        for (int i = 0; i < count; i++) {
            float angle = orbit + i * ((float) (Math.PI * 2.0D / count));
            float bob = (float) Math.sin(time * 0.12F + i * 0.8F) * 0.045F;
            pose.pushPose();
            pose.translate(0.0D, 1.02D + bob + (i % 2 == 0 ? 0.08D : -0.08D), 0.0D);
            pose.mulPose(Axis.YP.rotation(angle));
            pose.translate(0.0D, 0.0D, -radius);
            pose.mulPose(Axis.XP.rotation(-0.08F));
            renderShield(pose, out, time, i, red, green, blue);
            pose.popPose();
        }
    }

    private static void renderShield(PoseStack pose, VertexConsumer out, float time,
                                     int index, int red, int green, int blue) {
        Matrix4f m = pose.last().pose();
        float pulse = 0.82F + (float) Math.sin(time * 0.16F + index) * 0.12F;
        int alpha = 105 + (int) (pulse * 35.0F);
        quad(out, m, -0.23F,0.31F,0, 0.23F,0.31F,0, 0.18F,-0.05F,0, -0.18F,-0.05F,0, red,green,blue,alpha);
        quad(out, m, -0.18F,-0.05F,0, 0.18F,-0.05F,0, 0,-0.36F,0, 0,-0.36F,0, red/2,green/2,blue,alpha+12);
        quad(out, m, -0.12F,0.19F,-0.006F, 0.12F,0.19F,-0.006F, 0.09F,-0.035F,-0.006F, -0.09F,-0.035F,-0.006F,
                Math.min(255,red+80),Math.min(255,green+80),Math.min(255,blue+25),105);
        bar(out,m,-0.25F,0.33F,0.25F,0.33F,0.018F,Math.min(255,red+80),Math.min(255,green+80),255,235);
        bar(out,m,-0.25F,0.33F,0,-0.39F,0.018F,Math.min(255,red+55),Math.min(255,green+50),255,230);
        bar(out,m,0.25F,0.33F,0,-0.39F,0.018F,Math.min(255,red+55),Math.min(255,green+50),255,230);
        bar(out,m,-0.13F,0.06F,0.13F,0.06F,0.012F,190,235,255,220);
    }

    private static void renderRing(PoseStack pose, VertexConsumer out, float y, float radius,
                                   float thickness, int segments, int red, int green, int blue,
                                   int alpha, float rotation) {
        Matrix4f m = pose.last().pose();
        for (int i=0;i<segments;i++) {
            double a=rotation + Math.PI*2.0D*i/segments;
            double b=rotation + Math.PI*2.0D*(i+1)/segments;
            float x1=(float)Math.cos(a)*radius, z1=(float)Math.sin(a)*radius;
            float x2=(float)Math.cos(b)*radius, z2=(float)Math.sin(b)*radius;
            float nx1=(float)Math.cos(a)*(radius-thickness), nz1=(float)Math.sin(a)*(radius-thickness);
            float nx2=(float)Math.cos(b)*(radius-thickness), nz2=(float)Math.sin(b)*(radius-thickness);
            quad(out,m,x1,y,z1,x2,y,z2,nx2,y,nz2,nx1,y,nz1,red,green,blue,alpha);
        }
    }

    private static void renderVerticalAura(PoseStack pose, VertexConsumer out, float time,
                                           float radius, float thickness, int red, int green, int blue, int alpha) {
        Matrix4f m=pose.last().pose();
        for(int i=0;i<48;i++) {
            double a=Math.PI*2.0D*i/48.0D + time*0.012D;
            float x=(float)Math.cos(a)*radius, z=(float)Math.sin(a)*radius;
            float y1=0.22F+(float)(i%6)*0.27F, y2=y1+0.22F;
            float ox=-(float)Math.sin(a)*thickness, oz=(float)Math.cos(a)*thickness;
            quad(out,m,x+ox,y1,z+oz,x+ox,y2,z+oz,x-ox,y2,z-oz,x-ox,y1,z-oz,red,green,blue,alpha);
        }
    }

    private static void renderHalo(PoseStack pose, VertexConsumer out, float time, float y,
                                   float radius, int red, int green, int blue) {
        pose.pushPose();
        pose.translate(0,y,0);
        renderRing(pose,out,0,radius,0.025F,48,red,green,blue,210,time*0.025F);
        pose.popPose();
    }

    private static void renderWings(PoseStack pose, VertexConsumer out, float time) {
        float flap=(float)Math.sin(time*0.10F)*0.10F;
        Matrix4f m=pose.last().pose();
        for(int side=-1;side<=1;side+=2) {
            float s=side;
            quad(out,m,0.02F*s,1.42F,0.08F, 0.48F*s,1.82F+flap,0.12F,
                    0.92F*s,1.53F+flap,0.16F, 0.50F*s,1.24F,0.10F,255,225,135,90);
            for(int f=0;f<4;f++) {
                float x1=(0.28F+f*0.12F)*s;
                float x2=(0.53F+f*0.10F)*s;
                float y1=1.55F-f*0.055F;
                bar(out,m,x1,y1,x2,y1-0.18F+flap,0.018F,255,245,190,190);
            }
        }
    }

    private static void renderRisingSpiral(PoseStack pose, VertexConsumer out, float time,
                                           float radius, int red, int green, int blue) {
        Matrix4f m=pose.last().pose();
        for(int i=0;i<42;i++) {
            float t=i/41.0F;
            double a=time*0.075D+t*Math.PI*4.0D;
            float r=radius*(0.55F+0.45F*t);
            float x=(float)Math.cos(a)*r, z=(float)Math.sin(a)*r;
            float y=0.15F+t*1.8F;
            float nx=(float)Math.cos(a+0.12D)*r, nz=(float)Math.sin(a+0.12D)*r;
            quad(out,m,x,y,z,nx,y+0.045F,nz,nx,y+0.09F,nz,x,y+0.045F,z,red,green,blue,155);
        }
    }

    private static void renderVerticalBurst(PoseStack pose, VertexConsumer out, float time,
                                             float radius, int red, int green, int blue, int alpha) {
        Matrix4f m=pose.last().pose();
        for(int i=0;i<12;i++) {
            double a=Math.PI*2.0D*i/12.0D+time*0.025D;
            float x=(float)Math.cos(a), z=(float)Math.sin(a);
            bar(out,m,x*0.30F,0.15F,x*radius,0.15F,0.025F,red,green,blue,alpha);
            bar(out,m,x*radius,0.15F,x*radius*0.72F,1.45F,0.018F,red,green,blue,alpha-35);
        }
    }

    private static void renderInterceptTether(PoseStack pose, VertexConsumer out,
                                              CompanionEntity companion, float time) {
        Player owner=companion.level().players().stream()
                .filter(player -> player.getUUID().equals(companion.getOwnerUUID()))
                .findFirst().orElse(null);
        if(owner==null) return;
        double dx=owner.getX()-companion.getX();
        double dz=owner.getZ()-companion.getZ();
        double distance=Math.sqrt(dx*dx+dz*dz);
        if(distance<0.1D || distance>12.5D) return;
        float width=0.035F+(float)Math.sin(time*0.5F)*0.008F;
        Matrix4f m=pose.last().pose();
        float ox=(float)(-dz/distance)*width, oz=(float)(dx/distance)*width;
        float ex=(float)dx, ez=(float)dz;
        quad(out,m,ox,1.0F,oz,ex+ox,1.0F,ez+oz,ex-ox,1.0F,ez-oz,-ox,1.0F,-oz,75,190,255,225);
        quad(out,m,ox,1.08F,oz,ex+ox,1.08F,ez+oz,ex-ox,1.08F,ez-oz,-ox,1.08F,-oz,185,235,255,145);
    }

    private static void renderHolySlash(PoseStack pose, VertexConsumer out, float time) {
        Matrix4f m=pose.last().pose();
        float pulse=0.82F+(float)Math.sin(time*0.42F)*0.12F;
        // Wide segmented crescent with a bright inner edge and a soft outer blade.
        for(int i=0;i<28;i++) {
            float t0=i/28.0F, t1=(i+1)/28.0F;
            double a0=-Math.PI*0.78D+t0*Math.PI*0.92D;
            double a1=-Math.PI*0.78D+t1*Math.PI*0.92D;
            float rOuter=0.78F, rInner=0.58F;
            float x0=0.42F+(float)Math.cos(a0)*rOuter;
            float y0=1.02F+(float)Math.sin(a0)*rOuter;
            float x1=0.42F+(float)Math.cos(a1)*rOuter;
            float y1=1.02F+(float)Math.sin(a1)*rOuter;
            float ix1=0.42F+(float)Math.cos(a1)*rInner;
            float iy1=1.02F+(float)Math.sin(a1)*rInner;
            float ix0=0.42F+(float)Math.cos(a0)*rInner;
            float iy0=1.02F+(float)Math.sin(a0)*rInner;
            quad(out,m,x0,y0,-0.39F,x1,y1,-0.39F,ix1,iy1,-0.39F,ix0,iy0,-0.39F,
                    255,205,65,(int)(135*pulse));
            quad(out,m,x0,y0,-0.405F,x1,y1,-0.405F,ix1,iy1,-0.405F,ix0,iy0,-0.405F,
                    255,247,185,(int)(220*pulse));
        }
        // Impact flare at the end of the slash.
        double end=-Math.PI*0.78D+Math.PI*0.92D;
        float ex=0.42F+(float)Math.cos(end)*0.70F;
        float ey=1.02F+(float)Math.sin(end)*0.70F;
        for(int i=0;i<8;i++) {
            double angle=i*Math.PI/4.0D+time*0.08D;
            float dx=(float)Math.cos(angle), dy=(float)Math.sin(angle);
            bar(out,m,ex,ey,ex+dx*0.22F,ey+dy*0.22F,0.025F,255,235,145,205);
        }
    }

    private static void bar(VertexConsumer out, Matrix4f m, float x1, float y1,
                            float x2, float y2, float width, int r, int g, int b, int a) {
        float dx=x2-x1, dy=y2-y1;
        float length=(float)Math.sqrt(dx*dx+dy*dy);
        if(length<0.0001F) return;
        float ox=-dy/length*width, oy=dx/length*width;
        quad(out,m,x1+ox,y1+oy,-0.012F,x2+ox,y2+oy,-0.012F,
                x2-ox,y2-oy,-0.012F,x1-ox,y1-oy,-0.012F,r,g,b,a);
    }

    private static void quad(VertexConsumer out, Matrix4f m,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,
                             int r,int g,int b,int a) {
        vertex(out,m,x1,y1,z1,r,g,b,a); vertex(out,m,x2,y2,z2,r,g,b,a);
        vertex(out,m,x3,y3,z3,r,g,b,a); vertex(out,m,x4,y4,z4,r,g,b,a);
    }

    private static void vertex(VertexConsumer out, Matrix4f m, float x,float y,float z,
                               int r,int g,int b,int a) {
        out.vertex(m,x,y,z).color(r,g,b,a).endVertex();
    }
}
