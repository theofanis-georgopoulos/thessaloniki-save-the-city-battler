package controller;

import game.model.Character;
import game.model.*;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class BattleViewController {

    @FXML private VBox heroCard;
    @FXML private VBox enemyCard;
    @FXML private StackPane vsBadge;
    @FXML private Pane effectsPane;

    @FXML private StackPane mainMenuOverlay;

    // Hades-Style Cutscene & Choice Elements
    @FXML private StackPane cutsceneOverlay;
    @FXML private Label cutsceneTitleLabel;
    @FXML private Label cutsceneSpeakerLabel;
    @FXML private Label cutsceneSubtitleLabel;
    @FXML private Label cutsceneTextLabel;
    @FXML private ImageView cutscenePortraitImageView;
    @FXML private HBox portraitContainer;
    @FXML private StackPane portraitFrame;
    @FXML private VBox dialogueBoxFrame;
    @FXML private Button cutsceneNextBtn;

    @FXML private HBox choiceButtonsBox;
    @FXML private HBox standardNavBox;
    @FXML private Button choiceBtn1;
    @FXML private Button choiceBtn2;
    @FXML private Button choiceBtn3;

    @FXML private ImageView heroImageView;
    @FXML private ImageView enemyImageView;
    @FXML private Label heroNameLabel;
    @FXML private Label enemyNameLabel;
    @FXML private Label heroStatsLabel;
    @FXML private Label enemyStatsLabel;
    @FXML private Label stageLabel;
    @FXML private Label moralityLabel;

    @FXML private ProgressBar heroHpBar;
    @FXML private ProgressBar enemyHpBar;
    @FXML private Label battleLogLabel;

    @FXML private Button attackBtn;
    @FXML private Button specialBtn;
    @FXML private Button defendBtn;
    @FXML private Button summonBtn;
    @FXML private Button nextBattleBtn;

    private Character hero;
    private Character enemy;
    private boolean isPlayerTurn = true;
    private final Random random = new Random();

    private int specialCooldown = 0;
    private int summonCooldown = 0;

    // 1 έως 5 (Τα 5 Κεφάλαια της ιστορίας)
    private int currentChapter = 1;
    private int hopeScore = 0;
    private boolean showingPostFightEpilogue = false;

    // Wave / Minion Gauntlet Σύστημα
    private int currentWave = 1;
    private int maxWaves = 1;

    // Dialogue Buffs
    private int bonusAtk = 0;
    private int bonusDef = 0;
    private boolean startWithArmor = false;

    private ScaleTransition pulseBtnAnim;
    private final Map<String, AudioClip> soundCache = new HashMap<>();

    // Εσωτερική κλάση για δυναμικούς εχθρούς/ήρωες χωρίς θέματα με constructors
    private static class StageFighter extends BossDouros {
        private final String customName;
        private final int customMaxHp;
        private int customCurrentHp;
        private final int customAtk;
        private final int customDef;

        public StageFighter(String name, int maxHp, int attackPower, int defense) {
            super();
            this.customName = name;
            this.customMaxHp = maxHp;
            this.customCurrentHp = maxHp;
            this.customAtk = attackPower;
            this.customDef = defense;
        }

        @Override
        public String getName() { return customName; }

        @Override
        public int getMaxHp() { return customMaxHp; }

        @Override
        public int getCurrentHp() { return customCurrentHp; }

        @Override
        public int getAttackPower() { return customAtk; }

        @Override
        public int getDefense() { return customDef; }

        @Override
        public boolean isAlive() { return customCurrentHp > 0; }

        @Override
        public void takeDamage(int damage) {
            int effDef = (getStatus() == StatusEffect.WOOD_ARMOR) ? (int)(customDef * 1.5) : customDef;
            int actual = Math.max(1, damage - effDef);
            customCurrentHp = Math.max(0, customCurrentHp - actual);
        }

        @Override
        public void heal(int amount) {
            customCurrentHp = Math.min(customMaxHp, customCurrentHp + amount);
        }
    }

    // Δομή για κάθε γραμμή διαλόγου + προαιρετικές επιλογές (Choices)
    private static class DialogueLine {
        final String speaker;
        final String subtitle;
        final String text;
        final String imageKey;
        final String accentColor;
        final boolean alignRight;
        String[] choices;
        String[] choiceResponses;
        int[] moralityDeltas;

        DialogueLine(String speaker, String subtitle, String text, String imageKey, String accentColor, boolean alignRight) {
            this.speaker = speaker;
            this.subtitle = subtitle;
            this.text = text;
            this.imageKey = imageKey;
            this.accentColor = accentColor;
            this.alignRight = alignRight;
        }

        DialogueLine withChoices(String[] choices, String[] choiceResponses, int[] moralityDeltas) {
            this.choices = choices;
            this.choiceResponses = choiceResponses;
            this.moralityDeltas = moralityDeltas;
            return this;
        }
    }

    private final List<DialogueLine> activeDialogueQueue = new ArrayList<>();
    private int currentDialogueIndex = 0;

    @FXML
    public void initialize() {
        updateMoralityIndicator();
    }

    public void initBattle(String heroKey, String enemyKey) {
        if (mainMenuOverlay != null) {
            mainMenuOverlay.setVisible(true);
        }
    }

    public void initBattle(Character selectedHero, Character selectedEnemy) {
        this.hero = selectedHero;
        this.enemy = selectedEnemy;
        setupArenaUI();
    }

    @FXML
    private void openMainMenu() {
        if (mainMenuOverlay != null) {
            mainMenuOverlay.setVisible(true);
        }
    }

    @FXML
    private void startCampaignFromMenu() {
        if (mainMenuOverlay != null) {
            mainMenuOverlay.setVisible(false);
        }
        this.currentChapter = 1;
        this.hopeScore = 0;
        this.showingPostFightEpilogue = false;
        this.bonusAtk = 0;
        this.bonusDef = 0;
        this.startWithArmor = false;
        updateMoralityIndicator();
        startChapter(1);
    }

    private void startChapter(int chapter) {
        this.currentChapter = chapter;
        this.showingPostFightEpilogue = false;
        this.currentWave = 1;

        if (chapter == 2 || chapter == 3 || chapter == 4) {
            this.maxWaves = 2;
        } else {
            this.maxWaves = 1;
        }

        prepareChapterCharacters(chapter);
        buildChapterDialogue(chapter);
        showDialogueOverlay();
    }

    private void prepareChapterCharacters(int chapter) {
        Character baseHero;
        String heroDisplayName;

        if (chapter == 3 && currentWave == 2) {
            baseHero = new MrGoun();
            heroDisplayName = "Mr. Goun";
        } else if (chapter == 5) {
            baseHero = new MrGoun();
            heroDisplayName = "Captain Saver";
        } else {
            baseHero = new Nightgoon();
            heroDisplayName = baseHero.getName();
        }

        hero = new StageFighter(
                heroDisplayName,
                baseHero.getMaxHp(),
                baseHero.getAttackPower() + bonusAtk,
                baseHero.getDefense() + bonusDef
        );

        if (startWithArmor) {
            hero.setStatus(StatusEffect.WOOD_ARMOR);
        }

        if (currentWave == 1 && maxWaves > 1) {
            int minionHp = (chapter == 2) ? 75 : (chapter == 3 ? 85 : 95);
            int minionAtk = (chapter == 2) ? 14 : 16;
            enemy = new StageFighter("Douros's Minions", minionHp, minionAtk, 5);
        } else {
            if (chapter == 2) {
                enemy = new StageFighter("Minion Leader", 105, 18, 7);
            } else if (chapter == 3) {
                enemy = new StageFighter("Stazie", 120, 20, 8);
            } else if (chapter == 4) {
                enemy = new BossDouros();
            } else {
                enemy = new BossSunday();
            }
        }
    }

    private void buildChapterDialogue(int chapter) {
        activeDialogueQueue.clear();
        currentDialogueIndex = 0;

        if (chapter == 1) {
            cutsceneTitleLabel.setText("CHAPTER 1: Ο ΠΑΤΟΣ ΚΑΙ Η ΜΑΣΚΑ");

            activeDialogueQueue.add(new DialogueLine(
                    "NIGHTGOON", "• Ο ΠΑΤΟΣ",
                    "«Απόλυση το πρωί, έξωση το βράδυ. Η Θεσσαλονίκη σε ξερνάει έξω στο κρύο χωρίς δεύτερη σκέψη.»",
                    "nightgoon", "#7f8fa6", false
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "CHLOE", "• Η ΠΡΩΗΝ",
                    "«Κοίτα τα χάλια σου! Χωρίς φράγκα, χωρίς δουλειά, ένας σκέτος loser. Άσε με ησυχία, τελειώσαμε!»",
                    "chloe", "#f1c40f", true
            ).withChoices(
                    new String[]{"😠 Τσακώσου μαζί της", "🌧️ Παρακάλεσέ την να μείνει", "🚶 Φύγε σιωπηλά στη βροχή"},
                    new String[]{
                            "«Φύγε λοιπόν, Chloe! Όταν τα πράγματα δυσκολεύουν, κοιτάς μόνο τον εαυτό σου!» — Εκείνη γυρνάει την πλάτη.",
                            "«Περίμενε, Chloe...» — Εκείνη γελάει ειρωνικά και σε αφήνει μόνο στο βρεγμένο πεζοδρόμιο.",
                            "Δεν λες λέξη. Σφίγγεις τις γροθιές σου και την κοιτάς να χάνεται στη βροχή."
                    },
                    new int[]{-1, 0, 1}
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "CAPTAIN SAVER", "• Ο ΣΩΤΗΡΑΣ ΜΕ ΤΗΝ ΚΟΚΚΙΝΗ ΚΑΠΑ",
                    "Μια φιγούρα με κόκκινη κάπα κατεβαίνει στο βρεγμένο στενό.\n«Η πόλη σε πέταξε στα σκουπίδια, μικρέ. Αλλά η δικαιοσύνη δεν πεθαίνει ποτέ. Πάρε αυτή τη μάσκα και δείξε τους ποιος είσαι!»",
                    "captain_saver", "#00d2ff", false
            ).withChoices(
                    new String[]{"🤝 Πάρε το χέρι του Captain Saver (Hope)", "🔥 Δέξου τη μάσκα με οργή (Cynicism)"},
                    new String[]{
                            "Πιάνεις το χέρι του: «Η ιστορία μου δεν τελείωσε ακόμα, Captain Saver. Δείξε μου τον δρόμο.» (+Hope)",
                            "Πιάνεις το χέρι του: «Θα φορέσω τη μάσκα, αλλά με τον δικό μου τρόπο.» (+Cynicism)"
                    },
                    new int[]{2, -2}
            ));

        } else if (chapter == 2) {
            cutsceneTitleLabel.setText("CHAPTER 2: Η ΠΕΡΙΠΟΛΙΑ");

            activeDialogueQueue.add(new DialogueLine(
                    "CAPTAIN SAVER", "• Ο ΜΕΝΤΟΡΑΣ",
                    "«Η περιπολία ξεκινάει, Nightgoon. Δείξε αυτοσυγκράτηση. Οι δρόμοι θέλουν καρδιά, όχι μόνο γροθιές.»",
                    "captain_saver", "#00d2ff", false
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "MR. GOUN", "• Ο CULT ΔΙΚΟΣ ΜΑΣ ΑΝΘΡΩΠΟΣ",
                    "«Πού πάτε ρε κομάντος μέσα στη νύχτα; Προσέξτε τα στενά, η πόλη απόψε βρωμάει μπαρούτι!»",
                    "mrgoun", "#f5af19", true
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "kNotJohn & TUNG TUNG", "• ΟΙ ROOKIES",
                    "«Captain Saver! Nightgoon! Mr. Goun! Είμαστε έτοιμοι να καθαρίσουμε τη γειτονιά με τα ρόπαλά μας!»",
                    "knotjohn", "#a6e22e", true
            ).withChoices(
                    new String[]{"🛡️ Φέρσου σαν αυστηρός μεγάλος αδερφός", "🌟 Ενθάρρυνε τον ενθουσιασμό τους"},
                    new String[]{
                            "«Κάτω τα ρόπαλα, μικροί. Οι δρόμοι δεν είναι παιχνίδι.»",
                            "«Έχετε τη σωστή φλόγα, μάγκες! Μείνετε δίπλα μας.»"
                    },
                    new int[]{-1, 2}
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "NIGHTGOON", "• Η ΔΡΑΣΗ",
                    "«Ο Doctor Douros στέλνει τα τσιράκια του για να σπείρουν χάος στην παραλία. Ώρα να αναλάβω δράση!»",
                    "nightgoon", "#00d2ff", false
            ));

        } else if (chapter == 3) {
            cutsceneTitleLabel.setText("CHAPTER 3: Η ΕΝΕΔΡΑ ΤΗΣ STAZIE");

            activeDialogueQueue.add(new DialogueLine(
                    "DOCTOR DOUROS", "• Ο ΕΓΚΕΦΑΛΟΣ",
                    "«Χάος και αναρχία! Το Greek Dream διαλύεται στα εξ ων συνετέθη! Stazie, αποτελείωσέ τους!»",
                    "douros", "#a55eea", true
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "STAZIE", "• Η ΣΥΝΕΡΓΟΣ",
                    "«Ώρα να τσακίσουμε μερικά όνειρα, Doctor! Ας δούμε τι αξίζει αυτός ο Mr. Goun και ο Nightgoon!»",
                    "stazzie", "#ff5252", true
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "MR. GOUN", "• Ο ΗΡΩΑΣ ΤΗΣ ΜΑΧΗΣ",
                    "«Nightgoon, πρόσεξέ την! Η Stazie και ο Douros δεν θα περάσουν απόψε από εδώ!»",
                    "mrgoun", "#f5af19", false
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "NIGHTGOON", "• ΤΟ ΔΙΛΛΗΜΑ",
                    "Η Stazie μας επιτίθεται με λύσσα. Πώς θα κινηθούμε;",
                    "nightgoon", "#00d2ff", false
            ).withChoices(
                    new String[]{"🔥 Ορμήξτε στην ίδια τη Stazie (+ATK)", "🛡️ Πάρτε αμυντική διάταξη με τον Mr. Goun (+DEF)"},
                    new String[]{
                            "«Δώσε της να καταλάβει!» (+5 Attack)",
                            "«Κρατήστε άμυνα, έρχεται κύμα!» (+4 Defense)"
                    },
                    new int[]{-1, 2}
            ));

        } else if (chapter == 4) {
            cutsceneTitleLabel.setText("CHAPTER 4: Η ΠΡΟΔΟΣΙΑ ΤΗΣ SUNDAY");

            activeDialogueQueue.add(new DialogueLine(
                    "DOCTOR DOUROS", "• Η ΠΑΓΙΔΑ",
                    "«Χωρίσαμε τους ήρωες στα δύο! Ο Captain Saver έτρεξε πίσω από τη Sunday στην άλλη άκρη, και τώρα εσύ, Nightgoon, μαζί με τον Mr. Goun είστε ολομόναχοι!»",
                    "douros", "#a55eea", true
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "SUNDAY", "• Η ΜΟΙΡΑΙΑ ΓΥΝΑΙΚΑ",
                    "«Συγγνώμη, Captain Saver. Η αγάπη και οι σωτηρίες είναι για τα κορόιδα. Εγώ κοιτάω την πάρτη μου και σε αφήνω εκτεθειμένο στην παγίδα.»",
                    "sunday", "#e056fd", true
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "CAPTAIN SAVER", "• Ο ΠΛΗΓΩΜΕΝΟΣ ΗΡΩΑΣ",
                    "«Sunday... γιατί; Όλα όσα έκανα... θυσία για το τίποτα...» — Ο Captain Saver μένει αποσβολωμένος.",
                    "captain_saver", "#e74c3c", false
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "NIGHTGOON", "• ΜΟΝΟΣ ΣΤΟΝ ΑΓΩΝΑ",
                    "Ο Captain Saver παγιδεύτηκε. Πρέπει να βγάλουμε το φίδι από την τρύπα απέναντι στον Douros μόνοι μας με τον Mr. Goun!",
                    "nightgoon", "#00d2ff", false
            ).withChoices(
                    new String[]{"🏙️ Περιόρισε τις ζημιές (Ξεκίνα με Ασπίδα)", "⚔️ Ολική επίθεση στον Doctor Douros (+ATK)"},
                    new String[]{
                            "«kNotJohn, Tung! Κρατήστε γραμμή, αναλαμβάνω τον Douros!» (+Wood Armor)",
                            "«Δεν υπάρχει χρόνος! Χτυπήστε τον Douros με ό,τι έχετε!» (+5 Attack)"
                    },
                    new int[]{2, -2}
            ));

        } else if (chapter == 5) {
            cutsceneTitleLabel.setText("CHAPTER 5: Η ΤΕΛΙΚΗ ΑΝΑΜΕΤΡΗΣΗ ΤΟΥ CAPTAIN SAVER");

            activeDialogueQueue.add(new DialogueLine(
                    "CAPTAIN SAVER", "• Ο ΗΡΩΑΣ ΜΕ ΤΗΝ ΚΟΚΚΙΝΗ ΚΑΠΑ",
                    "«Αρκετά, Sunday. Με πρόδωσες, με πάτησες και νόμιζες ότι θα με διαλύσεις. Αλλά η κόκκινη κάπα δεν πέφτει τόσο εύκολα!»",
                    "captain_saver", "#00d2ff", false
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "SUNDAY", "• Η ΜΟΙΡΑΙΑ ΑΝΤΙΠΑΛΟΣ",
                    "«Ακόμα στέκεσαι όρθιος, Captain Saver; Νόμιζα ότι θα είχες φύγει κλαίγοντας. Ας δούμε αν αντέχεις την πραγματικότητα!»",
                    "sunday", "#e056fd", true
            ));

            activeDialogueQueue.add(new DialogueLine(
                    "MR. GOUN & NIGHTGOON", "• ΣΤΟ ΠΛΕΥΡΟ ΤΟΥ",
                    "«Captain Saver, είμαστε από πίσω σου! Τελείωσέ το μια για πάντα!»",
                    "mrgoun", "#f5af19", false
            ).withChoices(
                    new String[]{"🔥 Captain Saver: Πολέμησε με όλη σου τη δύναμη (+ATK)", "🛡️ Captain Saver: Κράτα την ψυχραιμία σου (+DEF)"},
                    new String[]{
                            "CAPTAIN SAVER: «Τέρμα τα λόγια, Sunday. Ώρα να λογαριαστούμε!» (+5 Attack)",
                            "CAPTAIN SAVER: «Δεν θα με παρασύρεις στο σκοτάδι σου. Στέκομαι ακλόνητος!» (+4 Defense)"
                    },
                    new int[]{2, 2}
            ));
        }
    }

    private void buildFinalRooftopEpilogue() {
        showingPostFightEpilogue = true;
        activeDialogueQueue.clear();
        currentDialogueIndex = 0;

        cutsceneTitleLabel.setText("EPILOGUE: Η ΔΙΚΑΙΩΣΗ ΣΤΗΝ ΤΑΡΑΤΣΑ");

        activeDialogueQueue.add(new DialogueLine(
                "CAPTAIN SAVER", "• ΟΡΘΙΟΣ ΣΤΟ ΤΕΛΟΣ",
                "Η Sunday ηττήθηκε. Ο Captain Saver στέκεται στην ταράτσα μαζί με τον Nightgoon και τον Mr. Goun. Η πόλη σώθηκε.",
                "captain_saver", "#00d2ff", false
        ).withChoices(
                new String[]{"🕯️ Συνέχισε ως ο Φύλακας της Ελπίδας", "🏙️ Παρέδωσε τη σκυτάλη στον Nightgoon"},
                new String[]{
                        "ENDING A: Ο Captain Saver ξαναβρίσκει την πίστη του και συνεχίζει να προστατεύει τη Θεσσαλονίκη μαζί με τη νέα γενιά!",
                        "ENDING B: Ο Captain Saver κρεμάει την κάπα δικαιωμένος και παραδίδει τα κλειδιά της πόλης στον Nightgoon και τον Mr. Goun!"
                },
                new int[]{5, 5}
        ));

        showDialogueOverlay();
    }

    private void showDialogueOverlay() {
        if (cutsceneOverlay == null) return;
        cutsceneOverlay.setOpacity(0.0);
        cutsceneOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(280), cutsceneOverlay);
        ft.setToValue(1.0);
        ft.play();

        displayDialogueLine(currentDialogueIndex);
    }

    private void displayDialogueLine(int index) {
        if (index < 0 || index >= activeDialogueQueue.size()) {
            finishDialoguePhase();
            return;
        }

        DialogueLine line = activeDialogueQueue.get(index);

        cutsceneSpeakerLabel.setText(line.speaker);
        cutsceneSpeakerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + line.accentColor + "; -fx-font-family: 'Impact';");

        if (cutsceneSubtitleLabel != null) {
            cutsceneSubtitleLabel.setText(line.subtitle);
        }

        cutsceneTextLabel.setText(line.text);

        if (dialogueBoxFrame != null) {
            dialogueBoxFrame.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #16162a, #0b0b16); " +
                            "-fx-background-radius: 14; -fx-border-color: " + line.accentColor + "; " +
                            "-fx-border-radius: 14; -fx-border-width: 3; -fx-padding: 20 28 20 28;"
            );
        }

        if (portraitFrame != null) {
            portraitFrame.setStyle(
                    "-fx-background-color: #10101e; -fx-background-radius: 16 16 0 0; " +
                            "-fx-border-color: " + line.accentColor + "; -fx-border-width: 3 3 0 3; -fx-border-radius: 16 16 0 0;"
            );
        }

        if (portraitContainer != null) {
            portraitContainer.setAlignment(line.alignRight ? Pos.BOTTOM_RIGHT : Pos.BOTTOM_LEFT);
        }

        if (cutscenePortraitImageView != null) {
            loadCharacterImage(cutscenePortraitImageView, line.imageKey);
        }

        boolean hasChoices = (line.choices != null && line.choices.length > 0);
        if (choiceButtonsBox != null && standardNavBox != null) {
            choiceButtonsBox.setVisible(hasChoices);
            choiceButtonsBox.setManaged(hasChoices);
            standardNavBox.setVisible(!hasChoices);
            standardNavBox.setManaged(!hasChoices);

            if (hasChoices) {
                setupChoiceButton(choiceBtn1, line.choices, 0);
                setupChoiceButton(choiceBtn2, line.choices, 1);
                setupChoiceButton(choiceBtn3, line.choices, 2);
            }
        }

        if (cutsceneNextBtn != null && !hasChoices) {
            updateCutsceneNextButtonText(index == activeDialogueQueue.size() - 1);
        }

        if (portraitFrame != null) {
            portraitFrame.setTranslateX(line.alignRight ? 40 : -40);
            portraitFrame.setOpacity(0.0);

            TranslateTransition slide = new TranslateTransition(Duration.millis(200), portraitFrame);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fade = new FadeTransition(Duration.millis(200), portraitFrame);
            fade.setToValue(1.0);

            new ParallelTransition(slide, fade).play();
        }
    }

    private void updateCutsceneNextButtonText(boolean isLastLine) {
        if (cutsceneNextBtn == null) return;
        if (isLastLine) {
            if (currentChapter == 1) cutsceneNextBtn.setText("ΠΡΟΣ CHAPTER 2 ➔");
            else if (showingPostFightEpilogue) cutsceneNextBtn.setText("ΤΕΛΟΣ ΙΣΤΟΡΙΑΣ ★");
            else cutsceneNextBtn.setText("ΣΤΗ ΜΑΧΗ! ⚔");
        } else {
            cutsceneNextBtn.setText("ΣΥΝΕΧΕΙΑ ➔");
        }
    }

    private void setupChoiceButton(Button btn, String[] choices, int idx) {
        if (btn == null) return;
        if (idx < choices.length) {
            btn.setText(choices[idx]);
            btn.setVisible(true);
            btn.setManaged(true);
        } else {
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }

    @FXML private void handleChoice1() { processPlayerChoice(0); }
    @FXML private void handleChoice2() { processPlayerChoice(1); }
    @FXML private void handleChoice3() { processPlayerChoice(2); }

    private void processPlayerChoice(int choiceIdx) {
        if (currentDialogueIndex >= activeDialogueQueue.size()) return;
        DialogueLine line = activeDialogueQueue.get(currentDialogueIndex);

        if (line.moralityDeltas != null && choiceIdx < line.moralityDeltas.length) {
            hopeScore += line.moralityDeltas[choiceIdx];
            updateMoralityIndicator();

            if (currentChapter == 3) {
                if (choiceIdx == 0) bonusAtk += 5;
                else if (choiceIdx == 1) bonusDef += 4;
            } else if (currentChapter == 4) {
                if (choiceIdx == 0) startWithArmor = true;
                else if (choiceIdx == 1) bonusAtk += 5;
            } else if (currentChapter == 5) {
                if (choiceIdx == 0) bonusAtk += 5;
                else if (choiceIdx == 1) bonusDef += 4;
            }
            prepareChapterCharacters(currentChapter);
        }

        if (line.choiceResponses != null && choiceIdx < line.choiceResponses.length) {
            cutsceneTextLabel.setText(line.choiceResponses[choiceIdx]);
        }

        line.choices = null;
        if (choiceButtonsBox != null && standardNavBox != null) {
            choiceButtonsBox.setVisible(false);
            choiceButtonsBox.setManaged(false);
            standardNavBox.setVisible(true);
            standardNavBox.setManaged(true);
        }

        updateCutsceneNextButtonText(currentDialogueIndex == activeDialogueQueue.size() - 1);
    }

    private void updateMoralityIndicator() {
        if (moralityLabel == null) return;
        if (hopeScore >= 2) {
            moralityLabel.setText("MORAL COMPASS: HOPEFUL (" + hopeScore + ")");
            moralityLabel.setStyle("-fx-text-fill: #00d2ff; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else if (hopeScore <= -2) {
            moralityLabel.setText("MORAL COMPASS: CYNICAL (" + hopeScore + ")");
            moralityLabel.setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            moralityLabel.setText("MORAL COMPASS: BALANCED (" + hopeScore + ")");
            moralityLabel.setStyle("-fx-text-fill: #f5af19; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleNextDialogueLine() {
        currentDialogueIndex++;
        if (currentDialogueIndex < activeDialogueQueue.size()) {
            displayDialogueLine(currentDialogueIndex);
        } else {
            finishDialoguePhase();
        }
    }

    private void finishDialoguePhase() {
        if (currentChapter == 1) {
            startChapter(2);
            return;
        }

        if (showingPostFightEpilogue) {
            cutsceneOverlay.setVisible(false);
            openMainMenu();
            return;
        }

        closeCutsceneAndFight();
    }

    @FXML
    private void skipCutsceneToFight() {
        finishDialoguePhase();
    }

    @FXML
    private void closeCutsceneAndFight() {
        if (cutsceneOverlay != null) {
            cutsceneOverlay.setVisible(false);
        }
        setupArenaUI();
        playIntroAnimation();
        if (isDouros(enemy)) {
            playSound("douros_intro");
        }
    }

    private void setupArenaUI() {
        isPlayerTurn = true;
        specialCooldown = 0;
        summonCooldown = 0;
        hideNextBattleButton();
        setButtonsEnabled(true);

        if (vsBadge != null) {
            vsBadge.setVisible(true);
        }

        stageLabel.setText("CHAPTER " + currentChapter + " • WAVE " + currentWave + "/" + maxWaves);

        heroHpBar.setProgress(1.0);
        enemyHpBar.setProgress(1.0);
        updateUI();
        updateSpecialButtonText();
        updateSummonButtonState();

        if (currentChapter == 3 && currentWave == 2) {
            loadCharacterImage(heroImageView, "mrgoun");
        } else if (currentChapter == 5) {
            loadCharacterImage(heroImageView, "captain_saver");
        } else {
            loadCharacterImage(heroImageView, "nightgoon");
        }

        if (currentWave == 1 && maxWaves > 1) {
            loadCharacterImage(enemyImageView, "minions");
        } else if (currentChapter == 2) {
            loadCharacterImage(enemyImageView, "minions");
        } else if (currentChapter == 3) {
            loadCharacterImage(enemyImageView, "stazzie");
        } else {
            loadCharacterImage(enemyImageView, enemy.getName());
        }

        battleLogLabel.setText("CHAPTER " + currentChapter + " (WAVE " + currentWave + "/" + maxWaves + "): " + enemy.getName() + "!");
    }

    @FXML
    private void handleBasicAttack() {
        if (!isPlayerTurn) return;
        isPlayerTurn = false;
        setButtonsEnabled(false);

        if (hero.getStatus() == StatusEffect.WOOD_ARMOR) {
            hero.setStatus(StatusEffect.NORMAL);
        }

        boolean wasDistracted = (hero.getStatus() == StatusEffect.DISTRACTED);

        int hpBefore = enemy.getCurrentHp();
        hero.performBasicAttack(enemy);
        int actualDamage = hpBefore - enemy.getCurrentHp();

        if (wasDistracted) {
            hero.setStatus(StatusEffect.NORMAL);
        }

        if (specialCooldown > 0) specialCooldown--;
        if (summonCooldown > 0) summonCooldown--;

        playAttackLunge(heroCard, true, () -> {
            playSound("punch");
            playHitEffect(enemyCard, "#ff1744");
            String dmgText = wasDistracted ? "-" + actualDamage + " (ΖΑΛΗ!)" : "-" + actualDamage + " HP";
            spawnFloatingText(705, 165, dmgText, "#ff5252");
            updateUI();
            battleLogLabel.setText("Ο " + hero.getName() + " επιτίθεται! Προκαλεί " + actualDamage + " ζημιά!");
            checkGameStatus();
        });
    }

    @FXML
    private void handleSpecialSkill() {
        if (!isPlayerTurn || specialCooldown > 0) return;
        isPlayerTurn = false;
        setButtonsEnabled(false);

        specialCooldown = 2;
        if (summonCooldown > 0) summonCooldown--;

        playCardGlow(heroCard, "#af7ac5");

        playAttackLunge(heroCard, true, () -> {
            playSound("special");
            int hpBefore = enemy.getCurrentHp();

            double multiplier = (hopeScore < 0) ? 1.95 : 1.75;
            int rawDmg = (int) (hero.getAttackPower() * multiplier);
            enemy.takeDamage(rawDmg);
            int actualDmg = hpBefore - enemy.getCurrentHp();

            if (hopeScore >= 0) {
                hero.heal(12);
                spawnFloatingText(195, 145, "+12 HP (HOPE)", "#2ecc71");
            }

            spawnFloatingText(705, 155, "SPECIAL -" + actualDmg + "!", "#e040fb");
            playHitEffect(enemyCard, "#e040fb");
            updateUI();
            battleLogLabel.setText("Εξαπολύεις Special επίθεση για " + actualDmg + " ζημιά!");
            checkGameStatus();
        });
    }

    @FXML
    private void handleDefend() {
        if (!isPlayerTurn) return;
        isPlayerTurn = false;
        setButtonsEnabled(false);

        if (specialCooldown > 0) specialCooldown--;
        if (summonCooldown > 0) summonCooldown--;

        playSound("shield");
        hero.setStatus(StatusEffect.WOOD_ARMOR);
        playCardGlow(heroCard, "#00d2ff");
        spawnFloatingText(195, 165, "+50% ΑΜΥΝΑ", "#00d2ff");
        updateUI();
        battleLogLabel.setText("Παίρνεις θέση άμυνας (+50% Defense)!");

        PauseTransition pause = new PauseTransition(Duration.seconds(1.0));
        pause.setOnFinished(event -> triggerEnemyTurn());
        pause.play();
    }

    @FXML
    private void handleSummonAssist() {
        if (!isPlayerTurn || summonCooldown > 0) return;
        summonKnotJohn();
    }

    private void summonKnotJohn() {
        isPlayerTurn = false;
        setButtonsEnabled(false);
        summonCooldown = 3;
        if (specialCooldown > 0) specialCooldown--;

        playSound("knotjohn");
        playSound("special");
        playCardGlow(heroCard, "#a6e22e");

        playAssistPopupAnimation(() -> {
            hero.heal(25);
            spawnFloatingText(195, 135, "+25 HP (BACKUP!)", "#2ecc71");

            int hpBefore = enemy.getCurrentHp();
            int assistDmg = (int) (hero.getAttackPower() * 1.4);
            enemy.takeDamage(assistDmg);
            enemy.setStatus(StatusEffect.DISTRACTED);
            int actualDmg = hpBefore - enemy.getCurrentHp();

            playHitEffect(enemyCard, "#a6e22e");
            spawnFloatingText(705, 145, "kNotJohn -" + actualDmg + " HP!", "#a6e22e");

            updateUI();
            battleLogLabel.setText("Ο kNotJohn μπαίνει στη μάχη! +25 HP και " + actualDmg + " ζημιά!");
            checkGameStatus();
        });
    }

    private void playAssistPopupAnimation(Runnable onStrike) {
        if (effectsPane == null) {
            if (onStrike != null) onStrike.run();
            return;
        }

        String borderColorHex = "#a6e22e";
        VBox assistBox = new VBox(10);
        assistBox.setAlignment(Pos.CENTER);
        assistBox.setPrefWidth(280);
        assistBox.setPrefHeight(340);
        assistBox.setStyle(
                "-fx-background-color: rgba(15, 22, 34, 0.98); " +
                        "-fx-border-color: " + borderColorHex + "; -fx-border-width: 4; " +
                        "-fx-background-radius: 18; -fx-border-radius: 18; -fx-padding: 16;"
        );

        DropShadow cyberGlow = new DropShadow(45, Color.web(borderColorHex));
        cyberGlow.setSpread(0.45);
        assistBox.setEffect(cyberGlow);

        Label title = new Label("⚡ ASSIST: kNotJohn ⚡");
        title.setStyle("-fx-text-fill: " + borderColorHex + "; -fx-font-weight: bold; -fx-font-size: 19px;");

        StackPane imgFrame = new StackPane();
        imgFrame.setPrefSize(240, 240);
        imgFrame.setStyle("-fx-background-color: #10141f; -fx-background-radius: 12; -fx-border-color: #2e4838; -fx-border-radius: 12; -fx-border-width: 2;");

        ImageView assistImg = new ImageView();
        assistImg.setFitWidth(225);
        assistImg.setFitHeight(225);
        assistImg.setPreserveRatio(true);
        loadCharacterImage(assistImg, "knotjohn");
        imgFrame.getChildren().add(assistImg);

        Label subTitle = new Label("ROOKIE BASEBALL BAT");
        subTitle.setStyle("-fx-text-fill: #00d2ff; -fx-font-weight: bold; -fx-font-size: 13px;");

        assistBox.getChildren().addAll(title, imgFrame, subTitle);

        assistBox.setLayoutX(-300);
        assistBox.setLayoutY(95);
        effectsPane.getChildren().add(assistBox);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(280), assistBox);
        slideIn.setToX(610);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        PauseTransition hold = new PauseTransition(Duration.millis(950));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), assistBox);
        fadeOut.setToValue(0.0);

        slideIn.setOnFinished(e -> {
            if (onStrike != null) onStrike.run();
            hold.play();
        });

        hold.setOnFinished(e -> fadeOut.play());
        fadeOut.setOnFinished(e -> effectsPane.getChildren().remove(assistBox));
        slideIn.play();
    }

    private void checkGameStatus() {
        if (!enemy.isAlive()) {
            if (currentWave < maxWaves) {
                playDefeatAnimation(enemyCard);
                playSound("punch");
                currentWave++;

                PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
                pause.setOnFinished(event -> {
                    prepareChapterCharacters(currentChapter);
                    setupArenaUI();
                    playIntroAnimation();
                    battleLogLabel.setText("ΠΡΟΣΟΧΗ! WAVE 2: Ο Boss " + enemy.getName() + " κάνει είσοδο!");
                });
                pause.play();
            } else {
                playDefeatAnimation(enemyCard);

                if (isDouros(enemy)) {
                    playSound("douros_defeat");
                } else {
                    playSound("victory");
                }

                if (currentChapter < 5) {
                    battleLogLabel.setText("ΝΙΚΗ ΣΤΟ CHAPTER " + currentChapter + "! Η ιστορία προχωράει...");
                    showNextBattleButton("ΠΡΟΣ CHAPTER " + (currentChapter + 1) + " ➔", true);
                } else {
                    battleLogLabel.setText("Η ΤΕΛΙΚΗ ΜΑΧΗ ΤΕΛΕΙΩΣΕ! Ο Captain Saver νίκησε... Ώρα για τον επίλογο στην ταράτσα!");
                    showNextBattleButton("ΕΠΙΛΟΓΟΣ (ΤΑΡΑΤΣΑ) ➔", true);
                }
            }
            return;
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(1.0));
        pause.setOnFinished(event -> triggerEnemyTurn());
        pause.play();
    }

    private void triggerEnemyTurn() {
        if (!enemy.isAlive()) return;

        boolean bossWasDistracted = (enemy.getStatus() == StatusEffect.DISTRACTED);
        int roll = random.nextInt(100);

        if (currentWave == 1 && maxWaves > 1) {
            executeBasicEnemyStrike("punch", "Τα τσιράκια του Douros σου ρίχνουν χτύπημα για ", bossWasDistracted);
            return;
        }

        if (isDouros(enemy)) {
            if (roll < 38 && !bossWasDistracted) {
                playCardGlow(enemyCard, "#ff0055");
                playAttackLunge(enemyCard, false, () -> {
                    playSound("douros_special");
                    int hpBefore = hero.getCurrentHp();
                    int specialDmg = (int) (enemy.getAttackPower() * 1.4);
                    hero.takeDamage(specialDmg);
                    int actualDamage = hpBefore - hero.getCurrentHp();

                    playHitEffect(heroCard, "#ff0055");
                    spawnFloatingText(195, 160, "ΧΑΟΣ -" + actualDamage + " HP!", "#ff0055");
                    updateUI();
                    battleLogLabel.setText("Χάος στους δρόμους! Χάνεις " + actualDamage + " HP!");
                    finishEnemyTurn();
                });
            } else {
                executeBasicEnemyStrike("douros_attack", "Ο " + enemy.getName() + " επιτίθεται για ", bossWasDistracted);
            }
            return;
        }

        if (roll < 42 && hero.getStatus() != StatusEffect.DISTRACTED && !bossWasDistracted) {
            playCardGlow(enemyCard, "#e040fb");
            playAttackLunge(enemyCard, false, () -> {
                playSound("sunday_charm");
                hero.setStatus(StatusEffect.DISTRACTED);
                int hpBefore = hero.getCurrentHp();
                hero.takeDamage((int) (enemy.getAttackPower() * 0.9));
                int actualDamage = hpBefore - hero.getCurrentHp();

                playHitEffect(heroCard, "#e040fb");
                spawnFloatingText(195, 150, "HEARTBREAK! (-50% ATK)", "#e040fb");
                updateUI();
                battleLogLabel.setText("Ο/Η " + enemy.getName() + " αποσυντονίζει τον ήρωα και κάνει " + actualDamage + " ζημιά!");
                finishEnemyTurn();
            });
        } else {
            executeBasicEnemyStrike("punch", "Ο/Η " + enemy.getName() + " αντεπιτίθεται για ", bossWasDistracted);
        }
    }

    private void executeBasicEnemyStrike(String soundKey, String logPrefix, boolean clearDistracted) {
        playAttackLunge(enemyCard, false, () -> {
            playSound(soundKey);
            int hpBefore = hero.getCurrentHp();
            enemy.performBasicAttack(hero);
            if (clearDistracted) enemy.setStatus(StatusEffect.NORMAL);
            int actualDamage = hpBefore - hero.getCurrentHp();

            playHitEffect(heroCard, "#ff1744");
            spawnFloatingText(195, 165, "-" + actualDamage + " HP", "#ff9100");
            updateUI();
            battleLogLabel.setText(logPrefix + actualDamage + " ζημιά!");
            finishEnemyTurn();
        });
    }

    private void finishEnemyTurn() {
        if (!hero.isAlive()) {
            playSound("defeat");
            playDefeatAnimation(heroCard);
            battleLogLabel.setText("ΗΤΤΑ! Έπεσες στη μάχη...");
            showNextBattleButton("ΕΠΑΝΑΛΗΨΗ ΜΑΧΗΣ ↻", false);
            return;
        }

        isPlayerTurn = true;
        setButtonsEnabled(true);
        updateSpecialButtonText();
        updateSummonButtonState();
    }

    private boolean isDouros(Character c) {
        return c != null && c.getName().toLowerCase().contains("douros") && !c.getName().toLowerCase().contains("minion");
    }

    private void playSound(String soundName) {
        try {
            if (soundCache.containsKey(soundName)) {
                soundCache.get(soundName).play();
                return;
            }
            String[] extensions = {".wav", ".mp3"};
            for (String ext : extensions) {
                File soundFile = new File("src/sounds/" + soundName + ext);
                if (soundFile.exists()) {
                    AudioClip clip = new AudioClip(soundFile.toURI().toString());
                    soundCache.put(soundName, clip);
                    clip.play();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void updateSpecialButtonText() {
        String skillName = (currentChapter == 3 && currentWave == 2) || (currentChapter == 5)
                ? "ΗΡΩΙΚΗ ΕΦΟΔΟΣ"
                : "ΣΚΟΤΕΙΝΗ ΕΝΕΔΡΑ";

        if (specialCooldown > 0) {
            specialBtn.setText(skillName + " (" + specialCooldown + ")");
            specialBtn.setDisable(true);
        } else {
            specialBtn.setText(skillName);
            if (isPlayerTurn) specialBtn.setDisable(false);
        }
    }

    private void updateSummonButtonState() {
        if (summonBtn == null) return;
        String assistName = "kNotJohn";
        if (summonCooldown > 0) {
            summonBtn.setText(assistName + " (" + summonCooldown + ")");
            summonBtn.setDisable(true);
        } else {
            summonBtn.setText("ΚΛΗΣΗ " + assistName);
            if (isPlayerTurn) summonBtn.setDisable(false);
        }
    }

    @FXML
    private void handleNextOrRetryBattle() {
        if (!hero.isAlive()) {
            prepareChapterCharacters(currentChapter);
            setupArenaUI();
        } else if (currentChapter < 5) {
            startChapter(currentChapter + 1);
        } else {
            buildFinalRooftopEpilogue();
        }
    }

    private void playIntroAnimation() {
        heroCard.setOpacity(1.0);
        enemyCard.setOpacity(1.0);
        heroCard.setRotate(0);
        enemyCard.setRotate(0);

        heroCard.setTranslateX(-120);
        enemyCard.setTranslateX(120);

        TranslateTransition heroIn = new TranslateTransition(Duration.millis(400), heroCard);
        heroIn.setToX(0);

        TranslateTransition enemyIn = new TranslateTransition(Duration.millis(400), enemyCard);
        enemyIn.setToX(0);

        new ParallelTransition(heroIn, enemyIn).play();
    }

    private void playAttackLunge(Node attackerCard, boolean isHero, Runnable onImpact) {
        double direction = isHero ? 55.0 : -55.0;

        TranslateTransition lungeForward = new TranslateTransition(Duration.millis(130), attackerCard);
        lungeForward.setToX(direction);
        lungeForward.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition lungeBack = new TranslateTransition(Duration.millis(180), attackerCard);
        lungeBack.setToX(0);
        lungeBack.setInterpolator(Interpolator.EASE_OUT);

        lungeForward.setOnFinished(e -> {
            if (onImpact != null) onImpact.run();
            lungeBack.play();
        });

        lungeForward.play();
    }

    private void playHitEffect(Node targetCard, String glowColorHex) {
        Timeline shake = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(targetCard.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(45), new KeyValue(targetCard.translateXProperty(), -14)),
                new KeyFrame(Duration.millis(90), new KeyValue(targetCard.translateXProperty(), 14)),
                new KeyFrame(Duration.millis(135), new KeyValue(targetCard.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(180), new KeyValue(targetCard.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(225), new KeyValue(targetCard.translateXProperty(), -5)),
                new KeyFrame(Duration.millis(270), new KeyValue(targetCard.translateXProperty(), 0))
        );
        shake.play();
        playCardGlow(targetCard, glowColorHex);
    }

    private void playCardGlow(Node card, String colorHex) {
        DropShadow hitGlow = new DropShadow(35, Color.web(colorHex));
        hitGlow.setSpread(0.55);
        card.setEffect(hitGlow);

        PauseTransition resetGlow = new PauseTransition(Duration.millis(320));
        resetGlow.setOnFinished(e -> {
            DropShadow normalShadow = new DropShadow(15, Color.rgb(0, 0, 0, 0.8));
            normalShadow.setSpread(0.2);
            card.setEffect(normalShadow);
        });
        resetGlow.play();
    }

    private void playDefeatAnimation(Node defeatedCard) {
        FadeTransition fade = new FadeTransition(Duration.millis(500), defeatedCard);
        fade.setToValue(0.35);

        RotateTransition tilt = new RotateTransition(Duration.millis(500), defeatedCard);
        tilt.setToAngle(defeatedCard == enemyCard ? 8 : -8);

        new ParallelTransition(fade, tilt).play();
    }

    private void spawnFloatingText(double centerX, double startY, String text, String colorHex) {
        if (effectsPane == null) return;

        Label floatingLabel = new Label(text);
        floatingLabel.setStyle(
                "-fx-font-size: 22px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + colorHex + "; " +
                        "-fx-effect: dropshadow(gaussian, black, 6, 0.8, 0, 2);"
        );

        floatingLabel.setLayoutX(centerX - 75);
        floatingLabel.setLayoutY(startY);
        effectsPane.getChildren().add(floatingLabel);

        TranslateTransition floatUp = new TranslateTransition(Duration.millis(900), floatingLabel);
        floatUp.setByY(-65);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(900), floatingLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ParallelTransition anim = new ParallelTransition(floatUp, fadeOut);
        anim.setOnFinished(e -> effectsPane.getChildren().remove(floatingLabel));
        anim.play();
    }

    private void animateHpBar(ProgressBar bar, double targetProgress) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(320), new KeyValue(bar.progressProperty(), targetProgress, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    private void showNextBattleButton(String text, boolean isVictory) {
        nextBattleBtn.setText(text);
        String bgGradient = isVictory
                ? "linear-gradient(to bottom, #2ecc71, #27ae60)"
                : "linear-gradient(to bottom, #e74c3c, #c0392b)";
        nextBattleBtn.setStyle(
                "-fx-background-color: " + bgGradient + "; " +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-border-color: #f1c40f; -fx-border-width: 2; -fx-border-radius: 12; -fx-cursor: hand;"
        );

        nextBattleBtn.setVisible(true);
        nextBattleBtn.setManaged(true);

        pulseBtnAnim = new ScaleTransition(Duration.millis(550), nextBattleBtn);
        pulseBtnAnim.setFromX(1.0);
        pulseBtnAnim.setFromY(1.0);
        pulseBtnAnim.setToX(1.08);
        pulseBtnAnim.setToY(1.08);
        pulseBtnAnim.setCycleCount(Animation.INDEFINITE);
        pulseBtnAnim.setAutoReverse(true);
        pulseBtnAnim.play();
    }

    private void hideNextBattleButton() {
        if (pulseBtnAnim != null) {
            pulseBtnAnim.stop();
        }
        nextBattleBtn.setScaleX(1.0);
        nextBattleBtn.setScaleY(1.0);
        nextBattleBtn.setVisible(false);
        nextBattleBtn.setManaged(false);
    }

    private void updateUI() {
        heroNameLabel.setText(hero.getName() + " [" + hero.getCurrentHp() + "/" + hero.getMaxHp() + "]");
        enemyNameLabel.setText(enemy.getName() + " [" + enemy.getCurrentHp() + "/" + enemy.getMaxHp() + "]");

        int heroDef = hero.getStatus() == StatusEffect.WOOD_ARMOR ? (int)(hero.getDefense() * 1.5) : hero.getDefense();
        int heroAtk = hero.getStatus() == StatusEffect.DISTRACTED ? hero.getAttackPower() / 2 : hero.getAttackPower();

        String statusTag = hero.getStatus() != StatusEffect.NORMAL ? " • " + hero.getStatus() : "";
        heroStatsLabel.setText("[ATK: " + heroAtk + " | DEF: " + heroDef + statusTag + "]");

        int enemyAtk = enemy.getStatus() == StatusEffect.DISTRACTED ? enemy.getAttackPower() / 2 : enemy.getAttackPower();
        String enemyStatusTag = enemy.getStatus() != StatusEffect.NORMAL ? " • " + enemy.getStatus() : "";
        enemyStatsLabel.setText("[ATK: " + enemyAtk + " | DEF: " + enemy.getDefense() + enemyStatusTag + "]");

        animateHpBar(heroHpBar, (double) hero.getCurrentHp() / hero.getMaxHp());
        animateHpBar(enemyHpBar, (double) enemy.getCurrentHp() / hero.getMaxHp());
    }

    private void loadCharacterImage(ImageView imageView, String key) {
        String[] candidates = getCandidateFilenames(key);
        for (String name : candidates) {
            File file = new File("src/images/" + name + ".png");
            if (!file.exists()) {
                file = new File("src/images/" + name + ".jpg");
            }
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
                return;
            }
        }
    }

    private String[] getCandidateFilenames(String key) {
        if (key == null) return new String[]{"nightgoon"};
        String k = key.toLowerCase().trim();
        if (k.contains("chloe")) return new String[]{"chloe", "sunday"};
        if (k.contains("minion")) return new String[]{"minions", "douros"};
        if (k.contains("saver") || k.contains("save_a_hoe")) return new String[]{"captain_saver", "captain_save_a_hoe", "saver", "mrgoun"};
        if (k.contains("goun") || k.contains("mr")) return new String[]{"mrgoun", "mr_goun", "goun"};
        if (k.contains("night") || k.contains("goon")) return new String[]{"nightgoon"};
        if (k.contains("douros")) return new String[]{"douros"};
        if (k.contains("staz")) return new String[]{"stazzie", "stazie", "sunday"};
        if (k.contains("sunday")) return new String[]{"sunday"};
        if (k.contains("tung")) return new String[]{"tung_tung", "tung"};
        if (k.contains("john") || k.contains("knot")) return new String[]{"knotjohn"};
        return new String[]{k};
    }

    private void setButtonsEnabled(boolean enabled) {
        attackBtn.setDisable(!enabled);
        defendBtn.setDisable(!enabled);
        if (!enabled) {
            specialBtn.setDisable(true);
            if (summonBtn != null) summonBtn.setDisable(true);
        } else {
            specialBtn.setDisable(specialCooldown > 0);
            updateSummonButtonState();
        }
    }
}