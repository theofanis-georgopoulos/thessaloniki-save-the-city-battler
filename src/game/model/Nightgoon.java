package game.model;

public class Nightgoon extends Hero {
    public Nightgoon() {
        super("H01", "Nightgoon", 120, 28, 12, 100);
    }

    @Override
    public boolean useSkill(Character target) {
        int cost = 35;
        if (this.energy < cost) return false;
        this.energy -= cost;
        target.takeDamage(this.attackPower * 2);
        return true;
    }
}