package es.clase.nochesuave;

import android.media.audiofx.DynamicsProcessing;

final class AudioConfiguration {
    static DynamicsProcessing.Config standard(float gain) {
        // Allocate the platform's standard six-band architecture. Bypass every
        // tone/dynamics stage so that only input gain changes the signal.
        DynamicsProcessing.Config config=new DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,2,
            true,6,true,6,true,6,true).setInputGainAllChannelsTo(gain).build();
        for(int index=0;index<2;index++) {
            DynamicsProcessing.Channel channel=config.getChannelByChannelIndex(index);
            channel.getPreEq().setEnabled(false);
            channel.getMbc().setEnabled(false);
            channel.getPostEq().setEnabled(false);
            channel.getLimiter().setEnabled(false);
            config.setChannelTo(index,channel);
        }
        return config;
    }
    static DynamicsProcessing.Config minimal(float gain) {
        return new DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,2,
            false,0,false,0,false,0,false).setInputGainAllChannelsTo(gain).build();
    }
}
