package bundlestash.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 读取合成书组件，用于判断合成书是否打开。 */
@Mixin(AbstractRecipeBookScreen.class)
public interface AbstractRecipeBookScreenAccess {

    @Accessor("recipeBookComponent")
    RecipeBookComponent<?> bundlestash$recipeBookComponent();
}
