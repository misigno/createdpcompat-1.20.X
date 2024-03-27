package net.misigno.createdpcompat.mixin;

import net.kyrptonaught.datapackportals.portalTypes.DefaultPortal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;


@Mixin(value = DefaultPortal.class)
public abstract class DefaultPortalMixin {

	@ModifyArg(at = @At(value = "INVOKE", target = "Lnet/kyrptonaught/customportalapi/api/CustomPortalBuilder;returnDim(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/kyrptonaught/customportalapi/api/CustomPortalBuilder;"), method = "toLink", index = 1)
	private boolean defaultIgnitionToTrue(boolean onlyIgnitableInReturnDim){
		return true;
	}
}
