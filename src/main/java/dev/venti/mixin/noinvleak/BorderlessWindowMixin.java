package dev.venti.mixin.noinvleak;

import dev.venti.modules.noinvleak.BorderlessFullscreen;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fullscreen becomes a borderless window covering the monitor while No Inv Leak's Stream Only asks for it. */
@Mixin(Window.class)
public abstract class BorderlessWindowMixin implements BorderlessFullscreen.Switchable {
   @Shadow
   @Final
   private long handle;
   @Shadow
   @Final
   private MonitorTracker monitorTracker;
   @Shadow
   private boolean fullscreen;
   @Shadow
   private boolean currentFullscreen;
   @Shadow
   private int windowedX;
   @Shadow
   private int windowedY;
   @Shadow
   private int windowedWidth;
   @Shadow
   private int windowedHeight;
   @Shadow
   private int x;
   @Shadow
   private int y;
   @Shadow
   private int width;
   @Shadow
   private int height;
   @Unique
   private boolean venti$borderless;

   @Inject(method = "updateWindowRegion()V", at = @At("HEAD"), cancellable = true)
   private void venti$setMode(CallbackInfo ci) {
      if (this.fullscreen && BorderlessFullscreen.wanted()) {
         Monitor monitor = this.monitorTracker.getMonitor((Window)(Object)this);
         if (monitor != null) {
            boolean exclusive = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
            if (!exclusive && !this.venti$borderless) {
               this.windowedX = this.x;
               this.windowedY = this.y;
               this.windowedWidth = this.width;
               this.windowedHeight = this.height;
            }

            VideoMode mode = monitor.getCurrentVideoMode();
            this.x = monitor.getViewportX();
            this.y = monitor.getViewportY();
            this.width = mode.getWidth();
            this.height = mode.getHeight();
            GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, GLFW.GLFW_DONT_CARE);
            if (exclusive) {
               GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            }

            this.venti$borderless = true;
            ci.cancel();
         }
      } else if (this.venti$borderless) {
         this.venti$leaveBorderless();
      }
   }

   @Unique
   private void venti$leaveBorderless() {
      this.venti$borderless = false;
      GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
      this.x = this.windowedX;
      this.y = this.windowedY;
      this.width = this.windowedWidth;
      this.height = this.windowedHeight;
   }

   @Override
   public boolean venti$isBorderless() {
      return this.venti$borderless;
   }

   @Override
   public void venti$reapplyMode() {
      this.currentFullscreen = !this.fullscreen;
   }
}
