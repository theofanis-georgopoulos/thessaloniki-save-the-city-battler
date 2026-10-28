package game.model;

import java.util.List;

public abstract class Enemy extends Character {
    protected int threatLevel;

    public Enemy(String id, String name, int maxHp, int attackPower, int defense, int threatLevel) {
        super(id, name, maxHp, attackPower, defense);
        this.threatLevel = threatLevel;
    }

    public abstract void executeAiTurn(List<Hero> activeHeroes);
}