package game.model;

public abstract class Hero extends Character {
    protected int energy;
    protected int maxEnergy;

    public Hero(String id, String name, int maxHp, int attackPower, int defense, int maxEnergy) {
        super(id, name, maxHp, attackPower, defense);
        this.maxEnergy = maxEnergy;
        this.energy = maxEnergy;
    }

    public abstract boolean useSkill(Character target);

    public void restoreEnergy(int amount) {
        this.energy = Math.min(this.energy + amount, maxEnergy);
    }

    public int getEnergy() { return energy; }
}