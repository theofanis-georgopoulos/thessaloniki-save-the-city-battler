package game.model;

import java.util.List;

public class BossDouros extends Enemy {
    public BossDouros() {
        super("E02", "Doctor Douros", 180, 30, 15, 5);
    }

    @Override
    public void executeAiTurn(List<Hero> activeHeroes) {
        // AoE επίθεση: χτυπά όλους τους ζωντανούς ήρωες
        for (Hero hero : activeHeroes) {
            if (hero.isAlive()) {
                hero.takeDamage(this.attackPower - 5);
            }
        }
    }
}