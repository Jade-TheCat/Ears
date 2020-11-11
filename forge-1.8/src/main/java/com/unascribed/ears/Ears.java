package com.unascribed.ears;

import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import com.unascribed.ears.common.AWTEarsImage;
import com.unascribed.ears.common.EarsCommon;
import com.unascribed.ears.common.EarsFeatures;
import com.unascribed.ears.common.EarsLog;

import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@Mod(modid="ears", name="Ears", version="@VERSION@", useMetadata=true, clientSideOnly=true)
public class Ears {
	
	public static final Map<ITextureObject, EarsFeatures> earsSkinFeatures = new WeakHashMap<>();
	
	private static boolean reentering;
	
	public Ears() {
		if (EarsLog.DEBUG) {
			EarsLog.debugva("Platform", "Initialized - {} / Forge {}; Side={}",
					Loader.instance().getMCVersionString(), ForgeVersion.getVersion(), FMLCommonHandler.instance().getSide());
		}
	}
	
	public static boolean interceptSetAreaOpaque(ImageBufferDownload subject, int x1, int y1, int x2, int y2) {
		EarsLog.debug("Platform:Inject", "stripAlpha({}, {}, {}, {}, {}) reentering={}", subject, x1, y1, x2, y2, reentering);
		if (reentering) return true;
		if (x1 == 0 && y1 == 0 && x2 == 32 && y2 == 16) {
			try {
				reentering = true;
				EarsCommon.carefullyStripAlpha((_x1, _y1, _x2, _y2) -> setAreaOpaque(subject, _x1, _y1, _x2, _y2), getImageHeight(subject) != 32);
			} finally {
				reentering = false;
			}
		}
		return false;
	}
	
	public static void checkSkin(ThreadDownloadImageData tdid, BufferedImage img) {
		EarsLog.debug("Platform:Inject", "Process player skin");
		earsSkinFeatures.put(tdid, EarsFeatures.detect(new AWTEarsImage(img)));
	}
	
	public static void addLayer(RenderPlayer rp) {
		EarsLog.debug("Platform:Inject", "Construct player renderer");
		rp.addLayer(new LayerEars(rp));
	}
	
	private static final MethodHandle setAreaOpaque;
	private static final MethodHandle imageHeight;
	static {
		try {
			Method jlr = ReflectionHelper.findMethod(ImageBufferDownload.class, null, new String[] {"func_78433_b", "setAreaOpaque"}, int.class, int.class, int.class, int.class);
			jlr.setAccessible(true);
			setAreaOpaque = MethodHandles.lookup().unreflect(jlr);
			
			Field ihf = ReflectionHelper.findField(ImageBufferDownload.class, "field_78437_c", "imageHeight");
			ihf.setAccessible(true);
			imageHeight = MethodHandles.lookup().unreflectGetter(ihf);
		} catch (Throwable t) {
			throw new Error(t);
		}
	}
	
	private static int getImageHeight(ImageBufferDownload subject) {
		try {
			return (int)imageHeight.invokeExact(subject);
		} catch (Throwable e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}
	private static void setAreaOpaque(ImageBufferDownload subject, int x1, int y1, int x2, int y2) {
		try {
			setAreaOpaque.invokeExact(subject, x1, y1, x2, y2);
		} catch (Throwable e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}
	
}
