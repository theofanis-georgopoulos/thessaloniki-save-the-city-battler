package game.model;

import java.util.List;

public class BossSunday extends Enemy {
    public BossSunday() {
        super("E01", "Sunday", 130, 22, 10, 4);
    }

    @Override
    public void executeAiTurn(List<Hero> activeHeroes) {
        // Αν ο Captain Saver είναι παρών, αποσπάται η προσοχή του
        for (Hero h : activeHeroes) {
            if (h instanceof CaptainSaver && h.isAlive()) {
                h.setStatus(StatusEffect.DISTRACTED);
            }
        }
        // Στοχεύει τον ήρωα με τα χαμηλότερα HP
        Hero target = activeHeroes.stream()
                .filter(Character::isAlive)
                .min((a, b) -> Integer.compare(a.getCurrentHp(), b.getCurrentHp()))
                .orElse(null);

        if (target != null) {
            performBasicAttack(target);
        }
    }
}