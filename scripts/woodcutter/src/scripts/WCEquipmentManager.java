package scripts;

import org.tribot.script.sdk.Equipment;
import org.tribot.script.sdk.tasks.Amount;
import org.tribot.script.sdk.tasks.BankTask;
import org.tribot.script.sdk.tasks.EquipmentReq;

import java.util.HashMap;
import java.util.Map;

public class WCEquipmentManager {

    private static Map<Equipment.Slot, Integer> equipment = new HashMap<>();

    public static BankTask getGear(){
        var equipmentBuilder = BankTask.builder();

        equipment.forEach((slot, integer) -> {
            equipmentBuilder.addEquipmentItem(EquipmentReq.slot(slot).item(integer, Amount.of(1)));
        });
        return equipmentBuilder.build();
    }

    public static boolean isGearSatisfied(){
        return getGear().isSatisfied();
    }

    public static void gearUp(){
        if (!isGearSatisfied()) {
            getGear().execute();
        }
    }

    public static Map<Equipment.Slot, Integer> getEquipment() {
        return equipment;
    }
}
