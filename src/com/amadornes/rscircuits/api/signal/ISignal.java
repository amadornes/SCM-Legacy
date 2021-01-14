package com.amadornes.rscircuits.api.signal;

import java.util.function.Supplier;

public interface ISignal<T> extends Supplier<T> {

    public interface IRedstoneSignal extends ISignal<Byte> {

        public boolean isStrong();

        public static IRedstoneSignal of(byte strength, boolean strong) {

            return new IRedstoneSignal() {

                @Override
                public Byte get() {

                    return strength;
                }

                @Override
                public boolean isStrong() {

                    return strong;
                }

            };
        }

    }

    public interface IBundledSignal extends ISignal<byte[]> {

    }

}
