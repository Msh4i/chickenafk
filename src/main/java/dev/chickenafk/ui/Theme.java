package dev.chickenafk.ui;

import dev.chickenafk.ChickenAfk;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** Black and white palette plus the bundled Geist fonts (rasters are set in assets/chickenafk/font/*.json). */
public final class Theme {
	public static final int BLACK = 0xFF000000;
	public static final int S2 = 0xFF121212;
	public static final int S3 = 0xFF1A1A1A;
	public static final int LINE = 0xFF222222;
	public static final int LINE2 = 0xFF333333;
	public static final int LINE3 = 0xFF4A4A4A;
	public static final int TEXT = 0xFFFAFAFA;
	public static final int SUB = 0xFFA3A3A3;
	public static final int MUTE = 0xFF5F5F5F;
	public static final int OK = 0xFF22C55E;
	public static final int WAIT = 0xFFF59E0B;
	public static final int OFF = 0xFF525252;

	private static final float[] SMALL = {1.0f, 1.4f, 2.0f, 2.8f};
	private static final float[] LARGE = {1.6f, 2.5f, 3.5f};
	private static final float[] HUGE = {1.0f, 1.6f, 2.5f};

	public static final Font SANS = new Font(rasters("sans", 13, SMALL), rasters("sans_l", 24, LARGE));
	public static final Font SEMI = new Font(rasters("semibold", 13, SMALL), rasters("semibold_l", 24, LARGE));
	public static final Font MONO = new Font(rasters("mono", 12, SMALL), rasters("mono_l", 44, HUGE));
	public static final Font DISPLAY = new Font(rasters("display", 52, HUGE));

	private Theme() {
	}

	/** One json per oversample, named name_1, name_2, ... in the order given. */
	private static List<Raster> rasters(String name, float size, float[] oversamples) {
		List<Raster> list = new ArrayList<>();
		for (int i = 0; i < oversamples.length; i++) {
			list.add(new Raster(name + "_" + (i + 1), size, oversamples[i]));
		}
		return list;
	}

	/**
	 * One json font: drawn at {@code size} GUI units per em, its glyphs rasterised at size x oversample
	 * pixels. Glyphs are sampled without smoothing, so they only look right drawn near that pixel size:
	 * bigger looks blocky, smaller drops thin strokes.
	 */
	public record Raster(FontDescription description, float size, float pixels) {
		Raster(String name, float size, float oversample) {
			this(new FontDescription.Resource(Identifier.fromNamespaceAndPath(ChickenAfk.MOD_ID, name)), size, size * oversample);
		}

		public Component text(String s) {
			return Component.literal(s).withStyle(style -> style.withFont(description));
		}
	}

	/**
	 * A typeface as a ladder of rasters. Vanilla's TTF rasteriser draws poorly at small pixel sizes, so
	 * text looks best from a raster about twice the pixels it covers, shrunk down; much more than that
	 * and thin strokes drop out, less and it turns blocky.
	 */
	public record Font(List<Raster> rasters) {
		private static final float SUPERSAMPLE = 1.8f;

		@SafeVarargs
		Font(List<Raster>... groups) {
			this(java.util.Arrays.stream(groups).flatMap(List::stream).toList());
		}

		public Raster pick(float screenPixels) {
			Raster best = rasters.getFirst();
			double bestDiff = Double.MAX_VALUE;
			for (Raster r : rasters) {
				double diff = Math.abs(Math.log(r.pixels() / (screenPixels * SUPERSAMPLE)));
				if (diff < bestDiff) {
					bestDiff = diff;
					best = r;
				}
			}
			return best;
		}
	}
}
