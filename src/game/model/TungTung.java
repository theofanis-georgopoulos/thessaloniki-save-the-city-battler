package game.model;

public class TungTung extends Hero {
    public TungTung() {
        super("H03", "Tung Tung Sahur", 150, 15, 25, 80);
        this.status = StatusEffect.WOOD_ARMOR;
    }

    @Override
    public boolean useSkill(Character target) {
        int cost = 30;
        if (this.energy < cost) return false;
        this.energy -= cost;
        this.status = StatusEffect.WOOD_ARMOR;
        return true;
    }
}