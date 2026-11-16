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

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics);
        int panelWidth=Math.min(520,Math.max(220,width-16));
        int panelHeight=Math.min(340,Math.max(220,height-16));
        int left=(width-panelWidth)/2, top=(height-panelHeight)/2;
        graphics.fill(left,top,left+panelWidth,top+panelHeight,0xF0181B20);
        graphics.fill(left,top,left+panelWidth,top+2,0xFF9A784A);
        graphics.fill(left,top+panelHeight-1,left+panelWidth,top+panelHeight,0xFF9A784A);
        graphics.drawCenteredString(font,title,width/2,top+10,0xFFFFE6BE);

        String className=data.classId>=0 && data.classId<CompanionClass.values().length
                ? CompanionClass.values()[data.classId].getDisplayName() : "Unknown";
        graphics.drawString(font,className+"  |  Level "+data.level,left+16,top+31,0xFFFFD58A,false);
        String xp=data.experienceToNext>0 ? data.experience+" / "+data.experienceToNext+" XP" : "MAX LEVEL";
        graphics.drawString(font,xp,left+panelWidth-16-font.width(xp),top+31,0xFFBFC5CC,false);

        int col1=left+18, col2=left+panelWidth/2+8;
        String bond = "Bond: " + data.relationshipTitle + "  |  " + data.relationshipExperience
                + (data.relationshipExperienceToNext > 0 ? " / " + data.relationshipExperienceToNext + " XP" : "  |  MAX");
        graphics.drawString(font,bond,left+16,top+45,0xFFE6C58E,false);

        graphics.drawString(font,"Title: " + data.companionTitle + "  |  Achievements: "
                + data.achievementCount + "/12",left+16,top+57,0xFFFFD58A,false);

        int y=top+75;
        graphics.drawString(font,"COMBAT",col1,y,0xFFFFE6BE,false);
        graphics.drawString(font,"PROGRESSION",col2,y,0xFFFFE6BE,false);
        y+=18;
        String[] labels={"Health","Attack Damage","Armor","Armor Toughness","Movement Speed","Knockback Resistance"};
        String[] values={number(stat(0))+" / "+number(stat(1)),number(stat(2)),number(stat(3)),
                number(stat(4)),number(stat(5)),number(stat(6)*100.0D)+"%"};
        for(int i=0;i<labels.length;i++) {
            graphics.drawString(font,labels[i],col1,y+i*17,0xFFBFC5CC,false);
            graphics.drawString(font,values[i],col1+112,y+i*17,0xFFFFFFFF,false);
        }

        int ay=top+93;
        for(CompanionAttribute attribute:CompanionAttribute.values()) {
            int value=attribute.ordinal()<data.attributes.length?data.attributes[attribute.ordinal()]:0;
            graphics.drawString(font,attribute.getDisplayName()+": "+value+"/20",
                    col2,ay,0xFFE8D2A8,false);
            ay+=17;
        }

        int skillsY=top+210;
        int skillRowStep=panelHeight<300?12:15;
        graphics.drawString(font,"UNLOCKED SKILLS",col1,skillsY,0xFFFFE6BE,false);
        int shown=0;
        for(CompanionSkill skill:CompanionSkill.values()) {
            if(skill.getArchetype().ordinal()!=data.classId) continue;
            int rank=skill.ordinal()<data.ranks.length?data.ranks[skill.ordinal()]:0;
            graphics.drawString(font,skill.getDisplayName()+"  "+rank+"/"+skill.getMaximumRank(),
                    col1+8,skillsY+17+shown*skillRowStep,rank>0?0xFFFFD58A:0xFF858B94,false);
            shown++;
        }
        graphics.drawCenteredString(font,"Values include equipped item modifiers",
                width/2,top+panelHeight-18,0xFF858B94);
        super.render(graphics,mouseX,mouseY,partialTick);
    }
}
