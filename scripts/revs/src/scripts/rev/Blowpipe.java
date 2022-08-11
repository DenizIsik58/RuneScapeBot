package scripts.rev;

import lombok.Getter;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Item;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.tasks.Amount;
import org.tribot.script.sdk.tasks.EquipmentReq;
import org.tribot.script.sdk.types.EquipmentItem;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.util.Retry;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyBanker;
import scripts.api.MyExchange;
import scripts.api.utility.MathUtility;
import scripts.api.utility.StringsUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static scripts.api.MyClient.clickWidget;

public class Blowpipe{

    public static Integer blowpipeId = 12926;
    public static Integer emptyBlowpipeId = 12924;
    public static Integer zulrahScaleId = 12934;
    public static Predicate<Item> isBlowpipe = item -> item.getId() == blowpipeId;
    public static Predicate<InventoryItem> isInventoryBlowpipe = item -> item.getId() == blowpipeId;
    public static Predicate<EquipmentItem> isEquipmentBlowpipe = equipmentItem -> equipmentItem.getId() == blowpipeId;

    private static final float maxScales = 16383;
    private static final float maxDartsForFull_Assembler = 4950;
    private static final float maxDartsForFull_Accumulator = 6900;

    @Getter
    public enum Dart {
        DRAGON(11230, "Dragon"),
        AMETHYST(25849, "Amethyst"),
        RUNE(811, "Rune"),
        ADAMANT(810, "Adamant"),
        MITHRIL(809, "Mithril"),
        STEEL(808, "Steel"),
        IRON(807, "Iron"),
        NONE(-1, "None");
        private final Integer id;
        private final String name;

        Dart(int id, String dartName) {
            this.id = id;
            this.name = dartName;
        }
    }

    public static EquipmentReq equipBlowpipe(Integer charges, Dart dartType) {
        loadCharges(charges, dartType);
        Waiting.waitNormal(500, 300);
        return EquipmentReq.slot(Equipment.Slot.WEAPON).item(blowpipeId, Amount.of(1));
    }

    public static Predicate<Item> blowpipe(Integer charges, Dart dartTpe) {
        loadCharges(charges, dartTpe);
        Waiting.waitNormal(500, 300);
        return gameObject -> gameObject.getId() == blowpipeId;
    }


    private static BlowpipeInfo getBlowpipeInfo(String string) {
        Dart dartType = Dart.NONE;
        int dartCount = 0;
        int scaleCount = 0;
        if (string == null || string.isEmpty() || string.isBlank())
            return new BlowpipeInfo(dartType, dartCount, scaleCount);
        Pattern dartPattern = Pattern.compile(">(\\w+ dart .*?)<");
        var dartScanner = new Scanner(string);
        var dartResults = dartScanner.findAll(dartPattern);
        var dartResult = dartResults.map(MatchResult::group).findFirst().orElse(null);
        if (dartResult != null) {
            for (Dart value : Dart.values()) {
                if (dartResult.contains(value.name)) dartType = value;
            }
            dartCount = Integer.parseInt(dartResult.replaceAll("\\D", ""));
        }
        Pattern scalePattern = Pattern.compile(">(\\d.*?)<");
        var scaleScanner = new Scanner(string);
        var scaleResults = scaleScanner.findAll(scalePattern);
        var scaleResult = scaleResults.map(MatchResult::group).findFirst().orElse(null);
        if (scaleResult != null)
            scaleCount = Integer.parseInt(scaleResult.replaceAll("\\((.*?)\\)", "").replaceAll("\\D", ""));
        return new BlowpipeInfo(dartType, dartCount, scaleCount);
    }

    public static BlowpipeInfo checkLastBlowpipeInfo() {
        var results = StringsUtility.getTextFromFirstLineContaining("Darts: ", "Scales: ");
        return getBlowpipeInfo(results);
    }

    public static BlowpipeInfo checkBlowpipe() {
        AtomicReference<String> checkResults = new AtomicReference<>("0");
        MyBanker.closeBank();
        Retry.retry(3, () -> {
            if (Inventory.contains(blowpipeId)) {
                MyBanker.closeBank();
                Query.inventory().filter(isInventoryBlowpipe).findFirst().ifPresent(blowpipe -> {
                    blowpipe.click("Check");
                    Waiting.wait(100);
                    var results = StringsUtility.getTextFromFirstLineContaining("Darts: ", "Scales: ");
                    checkResults.set(results);
                });
            } else if (Equipment.contains(blowpipeId))
                Query.equipment().filter(isEquipmentBlowpipe).findFirst().ifPresent(blowpipe -> {
                    blowpipe.click("Check");
                    Waiting.wait(100);
                    var results = StringsUtility.getTextFromFirstLineContaining("Darts: ", "Scales: ");
                    checkResults.set(results);
                });
            boolean success = checkResults.get() != null;
            if (!success) Waiting.waitNormal(400, 200);
            return success;
        });

        return getBlowpipeInfo(checkResults.get());
    }

    public static boolean loadCharges(Integer amount, Dart dartType) {
        return loadCharges(amount, dartType, null);
    }

    public static boolean loadCharges(Integer amount, Dart dartType, BlowpipeInfo info) {
        Log.debug("Loading charges");
        boolean wasBankOpen = Bank.isOpen();
        boolean wasEquipped = Equipment.contains(blowpipeId);
        AtomicInteger bankScaleCount = new AtomicInteger(0);
        AtomicInteger bankDartCount = new AtomicInteger(0);
        boolean depositScales = false;
        boolean depositDarts = false;

        if (!Inventory.contains(blowpipeId, emptyBlowpipeId) && !Equipment.contains(blowpipeId, emptyBlowpipeId)) {
            MyBanker.openBank();
            if (Bank.contains(blowpipeId)) {
                MyBanker.withdraw(blowpipeId, 1, false);
            } else if (Bank.contains(emptyBlowpipeId)) {
                Log.debug("Empty blowpipe found!");
                MyBanker.withdraw(emptyBlowpipeId, 1, false);
            } else {
                Log.error("No blowpipe found.");
                MyBanker.openBank();
                MyBanker.depositAll();
                GrandExchangeRevManager.sellBow();
                GrandExchangeRevManager.buyFromBank(emptyBlowpipeId, 1);
                return true;
            }
        }

        if (wasEquipped) {
            MyBanker.closeBank();
            Equipment.remove(blowpipeId);
            Waiting.waitUntil(() -> !Equipment.contains(blowpipeId));
        }


        if (info == null) {
            info = checkBlowpipe();
        }
        if (info.hasCharges(amount)) {
            Log.debug("I have enough charges");
            return true;
        }

        int dartsNeeded = info.getDartsNeededForCharges(amount);
        int scalesNeeded = info.getScalesNeededForCharges(amount);
        if (dartsNeeded > 0) {
            if (info.dartType != dartType && info.dartType != Dart.NONE) unloadPipe();
            int dartId = dartType.id;
            int dartPrice = Pricing.lookupPrice(dartId).orElse(2000);
            int inventoryAmount = Inventory.getCount(dartId);
            if (inventoryAmount >= dartsNeeded) {
                int excess = inventoryAmount - dartsNeeded;
                int excessCost = excess * dartPrice;
                if (excessCost > 50000) {
                    Bank.open();
                    Waiting.waitUntil(1000, Bank::isOpen);
                    var depositAmount = MathUtility.roundDownToNearest(excess, 50);
                    bankDartCount.set((int) depositAmount);
                    depositDarts = true;
                }
            } else {
                MyBanker.openBank();;
                var withdrawAmount = MathUtility.roundUpToNearest(dartsNeeded, 50);
                if (Bank.getCount(dartId) < dartsNeeded) {
                    Log.error("Not enough darts in bank.");
                    GrandExchangeRevManager.buyFromBank(dartId, dartsNeeded * 3);

                }
                Log.debug("Dart withdraw amount: " + withdrawAmount);
                bankDartCount.set((int) withdrawAmount);
            }
            Waiting.waitNormal(400, 200);
        }

        if (scalesNeeded > 0) {
            int scalePrice = Pricing.lookupPrice(zulrahScaleId).orElse(150);
            int inventoryAmount = Inventory.getCount(zulrahScaleId);
            if (inventoryAmount >= scalesNeeded) {
                int excess = inventoryAmount - scalesNeeded;
                int excessCost = excess * scalePrice;
                if (excessCost > 50000) {
                    MyBanker.openBank();
                    var depositAmount = MathUtility.roundDownToNearest(excess, 50);
                    bankScaleCount.set((int) depositAmount);
                    depositScales = true;
                }
            } else {
                MyBanker.openBank();
                var withdrawAmount = MathUtility.roundUpToNearest(scalesNeeded, 100);
                if (Bank.getCount(zulrahScaleId) < scalesNeeded) {
                    Log.error("Not enough scales in bank.");
                    GrandExchangeRevManager.buyFromBank(zulrahScaleId, scalesNeeded);

                }
                Log.debug("Scales withdraw amount: " + scalesNeeded);
                bankScaleCount.set((int) withdrawAmount);
            }
            Waiting.waitNormal(400, 200);
        }

        Log.debug(bankDartCount.get());
        Log.debug(bankScaleCount.get());
        Log.debug(depositDarts);
        Log.debug(depositDarts);
        if (bankDartCount.get() > 0 || bankScaleCount.get() > 0) {
            MyBanker.openBank();

            if (bankDartCount.get() > 0) {
                if (depositDarts) {
                    MyBanker.deposit(dartType.id, bankDartCount.get(), true);
                }
                else {
                    Log.debug("Trying to withdraw: " + dartType.name);
                    MyBanker.withdraw(dartType.id, bankDartCount.get(), true);
                }
            }
            if (bankScaleCount.get() > 0) {
                if (depositScales) {
                    MyBanker.deposit(zulrahScaleId, bankScaleCount.get(), true);
                } else {
                    MyBanker.withdraw(zulrahScaleId, bankScaleCount.get(), true);
                }
            }
        }
        
       MyBanker.closeBank();

        Query.inventory().idEquals(dartType.id).findFirst().ifPresent(darts -> {
            Query.inventory().idEquals(12924, 12926).findFirst().ifPresent(darts::useOn);
        });

        Waiting.waitNormal(400, 200);

        Query.inventory().idEquals(zulrahScaleId).findFirst().ifPresent(darts -> {
            Query.inventory().idEquals(12924, 12926).findFirst().ifPresent(darts::useOn);
        });

        MyBanker.openBank();
        Log.debug("Done loading");
        return checkLastBlowpipeInfo().hasCharges(amount);
    }

    public static boolean unloadPipe() {
        if (Inventory.contains(emptyBlowpipeId)) {
            return true;
        }
        if (!Inventory.contains(blowpipeId)) {
            return false;
        }
        AtomicBoolean success = new AtomicBoolean(false);
        Query.inventory().idEquals(blowpipeId).findFirst().ifPresent(pipe -> {
            success.set(pipe.click("Unload"));
        });
        return success.get();
    }

    public static boolean unchargePipe() {
        if (Inventory.contains(emptyBlowpipeId)) return true;
        if (!Inventory.contains(blowpipeId)) return false;
        AtomicBoolean success = new AtomicBoolean(false);
        Query.inventory().idEquals(blowpipeId).findFirst().ifPresent(pipe -> {
            pipe.click("Uncharge");
            Waiting.waitUntil(() -> Query.widgets().inIndexPath(584, 1).isVisible().findFirst().isPresent());
            success.set(Retry.retry(3, () -> {
                AtomicBoolean successful = new AtomicBoolean(false);
                Query.widgets().inIndexPath(584, 1).isVisible().findFirst().ifPresent(widget -> {
                    successful.set(widget.click("Yes"));
                    Waiting.waitNormal(400, 200);
                });
                return successful.get();
            }));
        });
        Waiting.waitNormal(400, 200);
        return success.get();
    }

    public static boolean hasAssembler() {
        List<Item> allItems = new ArrayList<>();
        allItems.addAll(Bank.getAll());
        allItems.addAll(Inventory.getAll());
        allItems.addAll(Equipment.getAll());
        return allItems.stream().anyMatch(item -> item.getId() == 22109);
    }

    @Getter
    public static class BlowpipeInfo {
        private final Dart dartType;
        private final Integer dartCount;
        private final Integer scaleCount;
        private float multiplier = 0;

        public BlowpipeInfo(Dart dartType, Integer dartCount, Integer scaleCount) {
            this.dartType = dartType;
            this.dartCount = dartCount;
            this.scaleCount = scaleCount;
        }

        public boolean hasCharges(Integer amount) {
            if (dartCount < amount) return false;
            if (multiplier == 0 || multiplier == maxScales / maxDartsForFull_Accumulator) {
                if (hasAssembler()) {
                    multiplier = maxScales / maxDartsForFull_Assembler;
                    Log.info("Found assembler, calculating charges with " + multiplier + " scales per dart.");
                } else {
                    multiplier = maxScales / maxDartsForFull_Accumulator;
                    Log.info("Did not find assembler, calculating charges with " + multiplier + " scales per dart.");
                }
            }
            var scalesRequired = amount * multiplier;
            Log.debug("Scales Required = " + scalesRequired);
            Log.debug("Scales count: " + scaleCount);
            Log.debug("Dart count: " + dartCount);
            return scaleCount >= scalesRequired;
        }

        public Integer getScalesNeededForCharges(Integer amount) {
            if (multiplier == 0 || multiplier == maxScales / maxDartsForFull_Accumulator) if (hasAssembler()) {
                multiplier = maxScales / maxDartsForFull_Assembler;
                Log.info("Found assembler, calculating charges with " + multiplier + " scales per dart.");
            } else {
                multiplier = maxScales / maxDartsForFull_Accumulator;
                Log.info("Did not find assembler, calculating charges with " + multiplier + " scales per dart.");
            }
            return Math.max(0, (int) (amount * multiplier) - scaleCount);
        }

        public Integer getDartsNeededForCharges(Integer amount) {
            return Math.max(0, amount - dartCount);
        }

    }

}
