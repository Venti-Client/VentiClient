package dev.venti.modules.emoji;

import dev.venti.config.BooleanSetting;
import dev.venti.config.StringListSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

/**
 * Turns Discord-style shortcodes in your outgoing chat into emoji: ":sob:" becomes the crying face.
 * Minecraft's built-in font draws these (as single-colour pixel glyphs), so everyone in chat sees them.
 */
public final class EmojiModule extends Module {
   private static final Pattern CODE = Pattern.compile(":([a-z0-9_+\\-]{1,32}):", Pattern.CASE_INSENSITIVE);
   private static final Map<String, String> BUILT_IN = new HashMap<>();
   private final BooleanSetting builtIn = this.add("built-in", new BooleanSetting("Built-in Emojis", true));
   private final StringListSetting custom = this.add("custom", new StringListSetting("Custom Emojis", "goat=🐐", "w=🏆"));
   private final BooleanSetting inCommands = this.add("commands", new BooleanSetting("Also In /msg And /r", true));

   public EmojiModule() {
      super("emojis", "Emojis", "Type :sob:, :skull:, :fire: and more in chat and they're sent as emoji. Add your own as name=emoji.", ModuleCategory.MISC);
      ClientSendMessageEvents.MODIFY_CHAT.register(message -> {
         EmojiModule module = Modules.get(EmojiModule.class);
         return module != null && module.isEnabled() ? module.convert(message) : message;
      });
      ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
         EmojiModule module = Modules.get(EmojiModule.class);
         if (module == null || !module.isEnabled() || !module.inCommands.get()) {
            return command;
         }

         // Only private-message commands carry free text worth converting; everything else stays exactly as typed.
         String lower = command.toLowerCase(Locale.ROOT);
         for (String prefix : new String[]{"msg ", "tell ", "w ", "whisper ", "r ", "reply ", "me "}) {
            if (lower.startsWith(prefix)) {
               return command.substring(0, prefix.length()) + module.convert(command.substring(prefix.length()));
            }
         }

         return command;
      });
   }

   /** Replaces every known :code: in the text; unknown codes (and lone colons) are left untouched. */
   public String convert(String text) {
      if (text.indexOf(':') < 0) {
         return text;
      }

      Map<String, String> table = this.table();
      Matcher m = CODE.matcher(text);
      StringBuilder out = new StringBuilder();
      while (m.find()) {
         String emoji = table.get(m.group(1).toLowerCase(Locale.ROOT));
         m.appendReplacement(out, Matcher.quoteReplacement(emoji != null ? emoji : m.group()));
      }

      m.appendTail(out);
      return out.toString();
   }

   private Map<String, String> table() {
      Map<String, String> table = this.builtIn.get() ? new HashMap<>(BUILT_IN) : new HashMap<>();
      for (String line : this.custom.get()) {
         int eq = line.indexOf('=');
         if (eq > 0 && eq < line.length() - 1) {
            table.put(line.substring(0, eq).strip().replace(":", "").toLowerCase(Locale.ROOT), line.substring(eq + 1).strip());
         }
      }

      return table;
   }

   private static void add(String emoji, String... names) {
      for (String name : names) {
         BUILT_IN.put(name, emoji);
      }
   }

   static {
      // Faces
      add("😀", "grinning");
      add("😃", "smiley");
      add("😄", "smile");
      add("😁", "grin");
      add("😆", "laughing", "satisfied");
      add("😅", "sweat_smile");
      add("😂", "joy");
      add("🤣", "rofl", "rolling_on_the_floor_laughing");
      add("🙂", "slight_smile", "slightly_smiling_face");
      add("🙃", "upside_down");
      add("😉", "wink");
      add("😊", "blush");
      add("😇", "innocent");
      add("🥰", "smiling_face_with_hearts");
      add("😍", "heart_eyes");
      add("🤩", "star_struck");
      add("😘", "kissing_heart");
      add("😋", "yum");
      add("😛", "stuck_out_tongue");
      add("😜", "stuck_out_tongue_winking_eye");
      add("🤪", "zany_face", "crazy");
      add("🤑", "money_mouth");
      add("🤗", "hugging", "hug");
      add("🤔", "thinking", "think");
      add("🤐", "zipper_mouth");
      add("🤨", "raised_eyebrow");
      add("😐", "neutral_face", "neutral");
      add("😑", "expressionless");
      add("😶", "no_mouth");
      add("😏", "smirk");
      add("😒", "unamused");
      add("🙄", "rolling_eyes", "eyeroll");
      add("😬", "grimacing");
      add("😌", "relieved");
      add("😔", "pensive");
      add("😪", "sleepy");
      add("😴", "sleeping");
      add("😷", "mask");
      add("🤒", "thermometer_face", "sick");
      add("🤢", "nauseated_face");
      add("🤮", "vomiting", "puke");
      add("🥵", "hot_face", "hot");
      add("🥶", "cold_face", "cold");
      add("🥴", "woozy_face", "woozy");
      add("😵", "dizzy_face");
      add("🤯", "exploding_head", "mind_blown");
      add("🤠", "cowboy");
      add("🥳", "partying_face", "party");
      add("😎", "sunglasses", "cool");
      add("🤓", "nerd");
      add("🧐", "monocle");
      add("😕", "confused");
      add("😟", "worried");
      add("🙁", "slight_frown", "slightly_frowning_face");
      add("☹️", "frowning", "frown");
      add("😮", "open_mouth");
      add("😯", "hushed");
      add("😲", "astonished");
      add("😳", "flushed");
      add("🥺", "pleading_face", "pleading");
      add("😦", "frowning_open_mouth");
      add("😨", "fearful");
      add("😰", "cold_sweat");
      add("😥", "disappointed_relieved");
      add("😢", "cry");
      add("😭", "sob");
      add("😱", "scream");
      add("😖", "confounded");
      add("😣", "persevere");
      add("😞", "disappointed", "sad");
      add("😓", "sweat");
      add("😩", "weary");
      add("😫", "tired_face");
      add("🥱", "yawning_face", "yawn");
      add("😤", "triumph");
      add("😡", "rage", "pout");
      add("😠", "angry");
      add("🤬", "cursing", "swearing");
      add("😈", "smiling_imp");
      add("👿", "imp");
      add("💀", "skull", "dead");
      add("☠️", "skull_and_crossbones");
      add("💩", "poop", "shit");
      add("🤡", "clown");
      add("👹", "ogre");
      add("👻", "ghost");
      add("👽", "alien");
      add("🤖", "robot");
      add("😺", "smiley_cat");
      add("😹", "joy_cat");
      add("😻", "heart_eyes_cat");
      add("🙀", "scream_cat");
      add("🙈", "see_no_evil");
      add("🙉", "hear_no_evil");
      add("🙊", "speak_no_evil");
      // Hearts and symbols
      add("❤️", "heart", "red_heart", "<3");
      add("🧡", "orange_heart");
      add("💛", "yellow_heart");
      add("💚", "green_heart");
      add("💙", "blue_heart");
      add("💜", "purple_heart");
      add("🖤", "black_heart");
      add("🤍", "white_heart");
      add("💔", "broken_heart");
      add("💕", "two_hearts");
      add("💖", "sparkling_heart");
      add("💯", "100");
      add("💢", "anger");
      add("💥", "boom", "collision");
      add("💫", "dizzy");
      add("💦", "sweat_drops");
      add("💨", "dash");
      add("💬", "speech_balloon");
      add("💤", "zzz");
      add("✨", "sparkles");
      add("⭐", "star");
      add("🌟", "star2", "glowing_star");
      add("🔥", "fire", "lit");
      add("⚡", "zap", "lightning");
      add("✅", "white_check_mark", "check");
      add("❌", "x", "cross");
      add("❗", "exclamation");
      add("❓", "question");
      add("⚠️", "warning");
      add("🚫", "no_entry_sign");
      // Hands and people
      add("👍", "thumbsup", "+1", "like");
      add("👎", "thumbsdown", "-1", "dislike");
      add("👌", "ok_hand", "ok");
      add("🤌", "pinched_fingers");
      add("✌️", "v", "peace");
      add("🤞", "crossed_fingers", "fingers_crossed");
      add("🤟", "love_you_gesture");
      add("🤘", "metal", "rock_on");
      add("🤙", "call_me");
      add("👈", "point_left");
      add("👉", "point_right");
      add("👆", "point_up_2");
      add("👇", "point_down");
      add("☝️", "point_up");
      add("✋", "raised_hand", "hand");
      add("👋", "wave");
      add("👏", "clap");
      add("🙌", "raised_hands");
      add("👐", "open_hands");
      add("🤝", "handshake");
      add("🙏", "pray", "please");
      add("💪", "muscle", "flex");
      add("👀", "eyes");
      add("🧠", "brain");
      add("🫡", "salute");
      add("🤷", "shrug");
      add("🤦", "facepalm");
      // Things
      add("🎉", "tada", "party_popper");
      add("🎁", "gift");
      add("🏆", "trophy");
      add("🥇", "first_place", "gold");
      add("👑", "crown");
      add("💎", "gem", "diamond");
      add("💰", "moneybag", "money");
      add("💸", "money_with_wings");
      add("⚔️", "crossed_swords", "swords");
      add("🗡️", "dagger");
      add("🛡️", "shield");
      add("🏹", "bow_and_arrow");
      add("⛏️", "pick", "pickaxe");
      add("🪓", "axe");
      add("🔪", "knife");
      add("💣", "bomb");
      add("🧨", "firecracker", "tnt");
      add("🎮", "video_game", "game");
      add("🎵", "musical_note", "music");
      add("🎧", "headphones");
      add("📸", "camera");
      add("🔒", "lock");
      add("🔑", "key");
      add("⏰", "alarm_clock");
      add("⌛", "hourglass");
      add("🚀", "rocket");
      add("🌍", "earth", "world", "globe");
      add("🌙", "crescent_moon", "moon");
      add("☀️", "sunny", "sun");
      add("🌈", "rainbow");
      add("❄️", "snowflake");
      add("🌧️", "rain");
      add("🌹", "rose");
      add("🌸", "cherry_blossom");
      add("🍀", "four_leaf_clover", "clover");
      add("🍎", "apple");
      add("🍌", "banana");
      add("🍉", "watermelon");
      add("🍕", "pizza");
      add("🍔", "hamburger", "burger");
      add("🍟", "fries");
      add("🍰", "cake");
      add("🍪", "cookie");
      add("☕", "coffee");
      add("🥤", "cup_with_straw", "drink");
      // Animals
      add("🐶", "dog");
      add("🐱", "cat");
      add("🐭", "mouse");
      add("🐰", "rabbit", "bunny");
      add("🦊", "fox");
      add("🐻", "bear");
      add("🐼", "panda");
      add("🐨", "koala");
      add("🐯", "tiger");
      add("🦁", "lion");
      add("🐮", "cow");
      add("🐷", "pig");
      add("🐸", "frog");
      add("🐵", "monkey_face");
      add("🐔", "chicken");
      add("🐧", "penguin");
      add("🐦", "bird");
      add("🦆", "duck");
      add("🦉", "owl");
      add("🐝", "bee");
      add("🦋", "butterfly");
      add("🐍", "snake");
      add("🐢", "turtle");
      add("🐙", "octopus");
      add("🦈", "shark");
      add("🐟", "fish");
      add("🐳", "whale");
      add("🦄", "unicorn");
      add("🐉", "dragon");
      add("🐐", "goat");
   }
}
