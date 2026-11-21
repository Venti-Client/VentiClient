package dev.venti.modules.world;

import dev.venti.module.Module;
import dev.venti.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fans world-render and totem-pop callbacks from the world mixins out to the enabled world modules. */
public final class WorldHooks {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static boolean rendering;

   private WorldHooks() {
   }

   /** Called once per frame inside the world pass, after the camera is set up. */
   public static void render3D(float partialTick) {
      if (rendering) {
         return;
      }

      rendering = true;
      try {
         for (Module module : Modules.all()) {
            if (module.isEnabled()) {
               try {
                  if (module instanceof BlockOverlayModule m) {
                     m.render3D(partialTick);
                  } else if (module instanceof OrbitOrbsModule m) {
                     m.render3D(partialTick);
                  } else if (module instanceof EffectParticlesModule m) {
                     m.render3D(partialTick);
                  } else if (module instanceof TotemGhostModule m) {
                     m.render3D(partialTick);
                  } else if (module instanceof dev.venti.modules.waypoints.WaypointsModule m) {
                     m.render3D(partialTick);
                  }
               } catch (RuntimeException e) {
                  LOGGER.error("[Venti] {} failed to render", module.name(), e);
               }
            }
         }
      } finally {
         rendering = false;
      }
   }

   /** Called on the client thread when the server reports a totem of undying popping. */
   public static void totemPop(LivingEntity entity) {
      if (entity == null || entity.isRemoved() || Minecraft.getInstance().level == null) {
         return;
      }

      EffectParticlesModule particles = Modules.get(EffectParticlesModule.class);
      if (particles != null && particles.isEnabled()) {
         particles.onTotemPop(entity);
      }

      TotemGhostModule spirit = Modules.get(TotemGhostModule.class);
      if (spirit != null && spirit.isEnabled()) {
         spirit.onTotemPop(entity);
      }
   }
}
