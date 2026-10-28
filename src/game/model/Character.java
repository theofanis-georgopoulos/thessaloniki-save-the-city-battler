package game.model;

public abstract class Character {
    protected String id;
    protected String name;
    protected int maxHp;
    protected int currentHp;
    protected int attackPower;
    protected int defense;
    protected boolean isAlive;
    protected StatusEffect status;

    public Character(String id, String name, int maxHp, int attackPower, int defense) {
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.attackPower = attackPower;
        this.defense = defense;
        this.isAlive = true;
        this.status = StatusEffect.NORMAL;
    }

    public int calculateDamageTaken(int rawDamage) {
        int effectiveDefense = this.defense;
        if (this.status == StatusEffect.WOOD_ARMOR) {
            effectiveDefense = (int) (effectiveDefense * 1.5);
        }
        int netDamage = rawDamage - effectiveDefense;
        return Math.max(netDamage, 1);
    }

    public void takeDamage(int amount) {
        int finalDamage = calculateDamageTaken(amount);
        this.currentHp -= finalDamage;
        if (this.currentHp <= 0) {
            this.currentHp = 0;
            this.isAlive = false;
        }
    }

    public int performBasicAttack(Character target) {
        int dmg = this.attackPower;
        if (this.status == StatusEffect.DISTRACTED) {
            dmg = dmg / 2;
        }
        target.takeDamage(dmg);
        return dmg;
    }

    public void setStatus(StatusEffect status) {
        this.status = status;
    }

    public String getName() { return name; }
    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    public boolean isAlive() { return isAlive; }
    public StatusEffect getStatus() { return status; }
    public String getId() { return id; }
    public int getAttackPower() { return attackPower; }
    public int getDefense() { return defense; }
    public void heal(int amount) {
        if (!this.isAlive) return;
        this.currentHp = Math.min(this.maxHp, this.currentHp + amount);
    }
}

