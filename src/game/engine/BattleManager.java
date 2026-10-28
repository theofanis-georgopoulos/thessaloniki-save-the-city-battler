package game.engine;

import game.model.Character;
import game.model.Hero;
import game.model.Enemy;
import game.model.CaptainSaver;
import java.util.ArrayList;
import java.util.List;

public class BattleManager {
    private List<Hero> heroes;
    private List<Enemy> enemies;
    private int currentChapter;
    private List<String> combatLog;

    public BattleManager(int chapter, List<Hero> heroes, List<Enemy> enemies) {
        this.currentChapter = chapter;
        this.heroes = heroes;
        this.enemies = enemies;
        this.combatLog = new ArrayList<>();

        if (chapter == 4) {
            heroes.removeIf(h -> h instanceof CaptainSaver);
            combatLog.add("Chapter 4: Captain Saver abandoned the team to chase Sunday! The remaining heroes must hold the line!");
        }
    }

    public boolean executePlayerTurn(Hero hero, String action, Character target) {
        if (!hero.isAlive()) return false;

        if ("ATTACK".equalsIgnoreCase(action)) {
            int dmg = hero.performBasicAttack(target);
            combatLog.add(hero.getName() + " attacked " + target.getName() + " for " + dmg + " damage.");
            return true;
        } else if ("SKILL".equalsIgnoreCase(action)) {
            boolean success = hero.useSkill(target);
            if (success) {
                combatLog.add(hero.getName() + " used special skill on " + target.getName() + ".");
            } else {
                combatLog.add(hero.getName() + " failed to use skill (insufficient energy or disabled).");
            }
            return success;
        }
        return false;
    }

    public void executeEnemiesTurn() {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                enemy.executeAiTurn(heroes);
                combatLog.add(enemy.getName() + " executed an attack.");
            }
        }
    }

    public String checkBattleStatus() {
        boolean anyHeroAlive = heroes.stream().anyMatch(Character::isAlive);
        boolean anyEnemyAlive = enemies.stream().anyMatch(Character::isAlive);

        if (!anyHeroAlive) return "DEFEAT";
        if (!anyEnemyAlive) return "VICTORY";
        return "ONGOING";
    }

    public List<Hero> getHeroes() { return heroes; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<String> getCombatLog() { return combatLog; }
}