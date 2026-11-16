package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.network.CompanionNetwork.StatsDataMessage;
import com.livingcompanion.progression.CompanionAttribute;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only, server-fed combat and progression summary for the companion. */
public final class CompanionStatsScreen extends Screen {
    private final Screen parent;
    private StatsDataMessage data;
    private int refreshTicks;

    private CompanionStatsScreen(Screen parent, StatsDataMessage data) {
        super(Component.literal("Companion Stats"));
        this.parent=parent;
        this.data=data;
    }

    public static void open(Screen parent, StatsDataMessage data) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new CompanionStatsScreen(parent,data));
    }

    public void updateData(StatsDataMessage next) {
        if(next.entityId==data.entityId) data=next;
    }

    @Override
    public void tick() {
        super.tick();
        if(++refreshTicks>=20) {
            refreshTicks=0;
            CompanionNetwork.requestStats(data.entityId);
        }
    }

    @Override
    public void onClose() {
        if(minecraft!=null) minecraft.setScreen(parent);
    }

    private double stat(int index) {
        return index>=0 && index<data.stats.length ? data.stats[index] : 0.0D;
    }

    private String number(double value) {
        return String.format(java.util.Locale.ROOT,"%.2f",value);
    }

    private String statValue(double total, double base) {
        return number(total) + " (" + (total-base>=0?"+":"") + number(total-base) + ")";
    }

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics);

        // Render the complete panel in a smaller virtual coordinate space. This
        // keeps the pixel font legible while giving every section room to breathe.
        final float uiScale=0.72F;
        graphics.pose().pushPose();
        graphics.pose().translate(width*(1.0F-uiScale)/2.0F,height*(1.0F-uiScale)/2.0F,0);
        graphics.pose().scale(uiScale,uiScale,1.0F);

        int virtualWidth=(int)(width/uiScale);
        int virtualHeight=(int)(height/uiScale);
        int panelWidth=Math.min(470,Math.max(220,virtualWidth-24));
        int panelHeight=Math.min(310,Math.max(250,virtualHeight-20));
        int left=(virtualWidth-panelWidth)/2, top=(virtualHeight-panelHeight)/2;
        graphics.fill(left,top,left+panelWidth,top+panelHeight,0xF0181B20);
        graphics.fill(left,top,left+panelWidth,top+2,0xFF9A784A);
        graphics.fill(left,top+panelHeight-1,left+panelWidth,top+panelHeight,0xFF9A784A);
        graphics.drawCenteredString(font,title,virtualWidth/2,top+9,0xFFFFE6BE);

        String className=data.classId>=0 && data.classId<CompanionClass.values().length
                ? CompanionClass.values()[data.classId].getDisplayName() : "Unknown";
        graphics.drawString(font,className+"  |  Level "+data.level,left+16,top+29,0xFFFFD58A,false);
        String xp=data.experienceToNext>0 ? data.experience+" / "+data.experienceToNext+" XP" : "MAX LEVEL";
        graphics.drawString(font,xp,left+panelWidth-16-font.width(xp),top+29,0xFFBFC5CC,false);

        String bond="Bond Lv. "+data.relationshipLevel+" — "+data.relationshipTitle+"  |  "+data.relationshipExperience
                +(data.relationshipExperienceToNext>0?" / "+data.relationshipExperienceToNext+" XP":"  |  MAX");
        graphics.drawString(font,bond,left+16,top+43,0xFFE6C58E,false);
        graphics.drawString(font,"Title: "+data.companionTitle+"  |  Achievements: "+data.achievementCount+"/12",
                left+16,top+56,0xFFFFD58A,false);

        int healthX=left+16, healthY=top+76, healthWidth=panelWidth-32;
        graphics.drawString(font,"HEALTH  "+number(stat(0))+" / "+number(stat(1)),
                healthX,healthY-12,0xFFFFE6BE,false);
        graphics.fill(healthX,healthY,healthX+healthWidth,healthY+7,0xFF090B0E);
        int healthFill=stat(1)>0?(int)(healthWidth*Math.max(0.0D,Math.min(1.0D,stat(0)/stat(1)))):0;
        graphics.fill(healthX,healthY,healthX+healthFill,healthY+7,0xFFB94D4D);

        int col1=left+16;
        int col2=left+panelWidth/2+8;
        int statY=top+98;
        graphics.drawString(font,"COMBAT",col1,statY,0xFFFFE6BE,false);
        graphics.drawString(font,"PROGRESSION",col2,statY,0xFFFFE6BE,false);

        String[] labels={"Health","Attack","Armor","Toughness","Move Speed","Knockback"};
        String[] values={
                statValue(stat(1),stat(7)),
                statValue(stat(2),stat(8)),
                statValue(stat(3),stat(9)),
                statValue(stat(4),stat(10)),
                statValue(stat(5),stat(11)),
                number(stat(6)*100.0D)+"% ("+(stat(6)-stat(12)>=0?"+":"")
                        +number((stat(6)-stat(12))*100.0D)+"%)"
        };
        int rowY=statY+16;
        for(int i=0;i<labels.length;i++) {
            int y=rowY+i*13;
            graphics.drawString(font,labels[i],col1,y,0xFFBFC5CC,false);
            graphics.drawString(font,values[i],col1+76,y,0xFFFFFFFF,false);
        }

        int attributeY=statY+16;
        for(CompanionAttribute attribute:CompanionAttribute.values()) {
            int value=attribute.ordinal()<data.attributes.length?data.attributes[attribute.ordinal()]:0;
            String line=attribute.getDisplayName()+": "+value+"/20";
            graphics.drawString(font,line,col2,attributeY,0xFFE8D2A8,false);
            int barX=col2, barY=attributeY+10, barWidth=Math.max(30,panelWidth/2-28);
            graphics.fill(barX,barY,barX+barWidth,barY+2,0xFF343941);
            graphics.fill(barX,barY,barX+(int)(barWidth*Math.max(0,Math.min(20,value))/20.0),barY+2,0xFFC49A5A);
            attributeY+=17;
        }

        int lowerY=top+198;
        graphics.drawString(font,"UNLOCKED SKILLS",col1,lowerY,0xFFFFE6BE,false);
        int shown=0;
        for(CompanionSkill skill:CompanionSkill.values()) {
            if(skill.getArchetype().ordinal()!=data.classId || skill.getRequiredSpecialization()!=null) continue;
            if(shown>=5) break;
            int rank=skill.ordinal()<data.ranks.length?data.ranks[skill.ordinal()]:0;
            graphics.drawString(font,skill.getDisplayName()+"  "+rank+"/"+skill.getMaximumRank(),
                    col1+8,lowerY+15+shown*12,rank>0?0xFFFFD58A:0xFF858B94,false);
            shown++;
        }

        graphics.drawString(font,"ACHIEVEMENTS",col2,lowerY,0xFFFFE6BE,false);
        int achievementRows=Math.min(6,data.achievementTitles.length);
        for(int i=0;i<achievementRows;i++) {
            graphics.drawString(font,data.achievementTitles[i],col2+8,lowerY+15+i*12,0xFFE8D2A8,false);
        }
        if(data.achievementTitles.length>achievementRows) {
            graphics.drawString(font,"+"+(data.achievementTitles.length-achievementRows)+" more",
                    col2+8,lowerY+15+achievementRows*12,0xFF9BA5AF,false);
        }

        String footer="Combat values include class, progression and equipment bonuses.";
        graphics.drawCenteredString(font,footer,virtualWidth/2,top+panelHeight-15,0xFF858B94);
        graphics.pose().popPose();
        super.render(graphics,mouseX,mouseY,partialTick);
    }
}
