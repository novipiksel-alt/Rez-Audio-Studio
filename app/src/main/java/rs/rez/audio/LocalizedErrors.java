// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

/** Presentation adapter: never exposes platform/provider diagnostics to the UI.
 * The unmodified engines retain their diagnostic contract and pure-Java tests.
 * Match against the Bosnian resource catalog, not hardcoded UI text.
 */
final class LocalizedErrors {
    private static final int[] MESSAGES = {
        R.string.file_unavailable,
        R.string.import_interrupted,
        R.string.import_space,
        R.string.no_duration,
        R.string.range_bounds,
        R.string.create_failed,
        R.string.write_unavailable,
        R.string.clip_unavailable,
        R.string.error_generic,
        R.string.save_interrupted,
        R.string.error_empty,
        R.string.error_wav_chunk,
        R.string.error_wav_format,
        R.string.error_wav_data,
        R.string.error_wav_extensible,
        R.string.error_wav_codec,
        R.string.error_wav_compressed,
        R.string.error_wav_layout,
        R.string.error_id3_invalid,
        R.string.error_id3_incomplete,
        R.string.error_frame,
        R.string.error_frame_incomplete,
        R.string.error_sample_rate,
        R.string.error_packet_format,
        R.string.error_ape,
        R.string.error_overwrite,
        R.string.error_time_bounds,
        R.string.error_short_clip,
        R.string.error_wav_limit,
        R.string.error_no_frames,
        R.string.error_codec,
        R.string.error_no_track,
        R.string.error_open,
        R.string.error_range_bounds,
        R.string.error_packet_size,
        R.string.error_encrypted,
        R.string.error_no_packets,
        R.string.error_platform_trim,
        R.string.error_end_before_start,
        R.string.error_time_format,
        R.string.error_time_parts,
        R.string.error_time_large,
        R.string.error_name,
    };
    static String message(Context context, Exception error) {
        String diagnostic = error.getMessage();
        if (diagnostic == null) return context.getString(R.string.error_generic);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocales(new android.os.LocaleList(Locale.forLanguageTag("bs")));
        Context bosnian = context.createConfigurationContext(config);
        for (int id : MESSAGES) {
            String template = bosnian.getString(id);
            if (template.contains("%1$s")) {
                int split = template.indexOf("%1$s");
                String before = template.substring(0, split), after = template.substring(split + 4);
                if (diagnostic.startsWith(before) && diagnostic.endsWith(after)
                        && diagnostic.length() >= before.length() + after.length()) {
                    String detail = diagnostic.substring(before.length(), diagnostic.length() - after.length());
                    return context.getString(id, detail);
                }
            } else if (diagnostic.equals(template) || diagnostic.equals(context.getString(id))) {
                return context.getString(id);
            }
        }
        return context.getString(R.string.error_generic);
    }
}
