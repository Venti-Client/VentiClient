package dev.venti.modules.view;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import org.lwjgl.glfw.GLFW;

/**
 * Turns the camera and the player separately while the freelook key (default Left Alt) is held or toggled.
 * The third-person camera still collides with blocks like vanilla.
 */
public final class FreelookModule extends Module {
   private static FreelookModule instance;
   private final ModeSetting keyMode = this.add("key-mode", new ModeSetting("Key Mode", "Hold", "Hold", "Toggle"));
   private final ModeSetting mode = this.add("mode", new ModeSetting("Mode", "Camera", "Camera", "Player"));
   private final BooleanSetting togglePerspective = this.add("toggle-perspective", new BooleanSetting("Toggle Perspective", true));
   private final NumberSetting sensitivity = this.add("camera-sensitivity", new NumberSetting("Camera Sensitivity", 8.0F, 0.0F, 10.0F).step(0.1F));
   private final BooleanSetting arrows = this.add("arrows-control-opposite", new BooleanSetting("Arrows Control Opposite", true));
   private final NumberSetting arrowSpeed = this.add("arrow-speed", new NumberSetting("Arrow Speed", 4.0F, 0.0F, 10.0F).step(0.5F));
   private final KeyBinding key;
   private boolean looking;
   private float cameraYaw;
   private float cameraPitch;
   private Perspective previousView;

   public FreelookModule() {
      super("freelook", "Freelook", "Hold the freelook key (Controls > Venti) to look around without turning.", ModuleCategory.VISUALS, true);
      this.sensitivity.visibleWhen(() -> this.mode.is("Camera"));
      this.arrowSpeed.visibleWhen(this.arrows::get);
      this.key = ViewKeys.register("key.venti.freelook", GLFW.GLFW_KEY_LEFT_ALT);
      instance = this;
   }

   @Override
   protected void onDisable() {
      this.stop();
   }

   @Override
   protected void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientPlayerEntity player = mc.player;
      if (player == null) {
         this.stop();
         while (this.key.wasPressed()) {
         }

         return;
      }

      if (this.keyMode.is("Hold")) {
         while (this.key.wasPressed()) {
         }

         boolean held = this.key.isPressed() && mc.currentScreen == null;
         if (held && !this.looking) {
            this.start(mc, player);
         } else if (!held && this.looking) {
            this.stop();
         }
      } else {
         while (this.key.wasPressed()) {
            if (this.looking) {
               this.stop();
            } else {
               this.start(mc, player);
            }
         }
      }

      if (this.looking) {
         if (this.arrows.get() && mc.currentScreen == null) {
            float turn = this.arrowSpeed.get();
            float yawTurn = (held(mc, GLFW.GLFW_KEY_RIGHT) ? turn : 0.0F) - (held(mc, GLFW.GLFW_KEY_LEFT) ? turn : 0.0F);
            float pitchTurn = (held(mc, GLFW.GLFW_KEY_DOWN) ? turn : 0.0F) - (held(mc, GLFW.GLFW_KEY_UP) ? turn : 0.0F);
            if (this.mode.is("Player")) {
               this.cameraYaw += yawTurn;
               this.cameraPitch += pitchTurn;
            } else if (yawTurn != 0.0F || pitchTurn != 0.0F) {
               player.setYaw(player.getYaw() + yawTurn);
               player.setPitch(MathHelper.clamp(player.getPitch() + pitchTurn, -90.0F, 90.0F));
            }
         }

         this.cameraPitch = MathHelper.clamp(this.cameraPitch, -90.0F, 90.0F);
      }
   }

   private void start(MinecraftClient mc, ClientPlayerEntity player) {
      this.looking = true;
      this.cameraYaw = player.getYaw();
      this.cameraPitch = player.getPitch();
      this.previousView = mc.options.getPerspective();
      if (this.previousView != Perspective.THIRD_PERSON_BACK && this.togglePerspective.get()) {
         mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
      }
   }

   private void stop() {
      if (this.looking) {
         this.looking = false;
         MinecraftClient mc = MinecraftClient.getInstance();
         if (this.previousView != null && mc.options.getPerspective() != this.previousView && this.togglePerspective.get()) {
            mc.options.setPerspective(this.previousView);
         }

         this.previousView = null;
      }
   }

   private static boolean held(MinecraftClient mc, int key) {
      return InputUtil.isKeyPressed(mc.getWindow(), key);
   }

   /** The running freelook, or null when the camera follows the player as usual. */
   public static FreelookModule looking() {
      FreelookModule module = instance;
      return module != null && module.isEnabled() && module.looking ? module : null;
   }

   public boolean cameraMode() {
      return this.mode.is("Camera");
   }

   public void turnCamera(double yRot, double xRot) {
      float divisor = Math.max(this.sensitivity.get(), 0.1F);
      this.cameraYaw += (float)(yRot / divisor);
      this.cameraPitch += (float)(xRot / divisor);
      this.cameraPitch = MathHelper.clamp(this.cameraPitch, -90.0F, 90.0F);
   }

   public float cameraYaw() {
      return this.cameraYaw;
   }

   public float cameraPitch() {
      return this.cameraPitch;
   }
}
