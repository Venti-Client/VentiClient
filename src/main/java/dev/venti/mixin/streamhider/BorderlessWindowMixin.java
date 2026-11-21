package dev.venti.mixin.streamhider;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import dev.venti.modules.streamhider.BorderlessFullscreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.2 {
import com.mojang.blaze3d.platform.MonitorManager;
//?} else {
/*import com.mojang.blaze3d.platform.ScreenManager;
*///?}

/** Fullscreen becomes a borderless window covering the monitor while No Inv Leak's Stream Only asks for it. */
//? if >=26.2 {
@Mixin(Window.class)
public abstract class BorderlessWindowMixin implements BorderlessFullscreen.Switchable {
   @Shadow
   @Final
   private long handle;
   @Shadow
   @Final
   private MonitorManager monitorManager;
   @Shadow
   private boolean fullscreen;
   @Shadow
   private boolean actuallyFullscreen;
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

   @Inject(method = "setMode()V", at = @At("HEAD"), cancellable = true)
   private void venti$setMode(CallbackInfo ci) {
      if (this.fullscreen && BorderlessFullscreen.wanted()) {
         Monitor monitor = this.monitorManager.findBestMonitor((Window)(Object)this);
         if (monitor != null) {
            boolean exclusive = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
            if (!exclusive && !this.venti$borderless) {
               this.windowedX = this.x;
               this.windowedY = this.y;
               this.windowedWidth = this.width;
               this.windowedHeight = this.height;
            }

            VideoMode mode = monitor.currentMode();
            this.x = monitor.x();
            this.y = monitor.y();
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
      this.actuallyFullscreen = !this.fullscreen;
   }
}
//?} else {
/*@Mixin(Window.class)
public abstract class BorderlessWindowMixin implements BorderlessFullscreen.Switchable {
   @Shadow
   @Final
   private long handle;
   @Shadow
   @Final
   private ScreenManager screenManager;
   @Shadow
   private boolean fullscreen;
   @Shadow
   private boolean actuallyFullscreen;
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

   @Inject(method = "setMode()V", at = @At("HEAD"), cancellable = true)
   private void venti$setMode(CallbackInfo ci) {
      if (this.fullscreen && BorderlessFullscreen.wanted()) {
         Monitor monitor = this.screenManager.findBestMonitor((Window)(Object)this);
         if (monitor != null) {
            boolean exclusive = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
            if (!exclusive && !this.venti$borderless) {
               this.windowedX = this.x;
               this.windowedY = this.y;
               this.windowedWidth = this.width;
               this.windowedHeight = this.height;
            }

            VideoMode mode = monitor.getCurrentMode();
            this.x = monitor.getX();
            this.y = monitor.getY();
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
      this.actuallyFullscreen = !this.fullscreen;
   }
}
*///?}
