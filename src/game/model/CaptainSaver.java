package game.model;

public class CaptainSaver extends Hero {
    public CaptainSaver() {
        super("H02", "Captain Saver", 200, 45, 20, 100);
    }

    @Override
    public boolean useSkill(Character target) {
        int cost = 50;
        if (this.energy < cost || this.status == StatusEffect.SHATTERED) return false;
        this.energy -= cost;
        target.takeDamage((int) (this.attackPower * 2.5));
        return true;
    }
}