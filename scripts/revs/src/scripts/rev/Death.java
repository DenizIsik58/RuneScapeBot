package scripts.rev;

import dax.walker.models.WaitCondition;
import org.tribot.script.sdk.ChatScreen;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;

public class Death {


    public static void talkToDeath(){
        if (Query.npcs().nameEquals("Death").isAny()){
            ChatScreen.setConfig(ChatScreen.Config.builder().holdSpaceForContinue(true)
                    .useKeysForOptions(true).build());
            // We are dead
            // Talk to death
            Query.npcs().nameEquals("Death").findRandom().map(death -> death.click("Talk-to"));
            // Wait until chatscreen is open
            Waiting.waitUntil(ChatScreen::isOpen);
            while (ChatScreen.isClickContinueOpen()) {
                ChatScreen.clickContinue();
                Waiting.wait(50);
            }

            // We need to cover option 1, 2, 3 and 4

            ChatScreen.handle("Tell me about gravestones again.", "How do I pay a gravestone fee?"
                    ,"How long do I have to return to my gravestone?",
                    "How do I know what will happen to my items when I die?");

            Query.gameObjects().idEquals(39549).findFirst().ifPresent(portal -> portal.click("Use"));
            Waiting.waitNormal(3000,200);

        }

    }
}
