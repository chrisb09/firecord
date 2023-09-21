package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.concurrent.LinkedBlockingQueue;

import org.javatuples.Triplet;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public abstract class NumericData<T extends Number> extends SmallData<T> {

    private enum MathOperator {

        ADD {
            @Override
            <E extends Number> E apply(NumericData<E> a, E b) {
                return a.add(b);
            }
        },
        MULTIPLY {
            @Override
            <E extends Number> E apply(NumericData<E> a, E b) {
                return a.mul(b);
            }
        },
        SUBTRACT {
            @Override
            <E extends Number> E apply(NumericData<E> a, E b) {
                return a.sub(b);
            }
        },
        DIVIDE {
            @Override
            <E extends Number> E apply(NumericData<E> a, E b) {
                return a.div(b);
            }
        };

        abstract <E extends Number> E apply(NumericData<E> a, E b);
    }

    private static LinkedBlockingQueue<Triplet<NumericData<Number>, Number, MathOperator>> asyncCalcQueue = new LinkedBlockingQueue<>();

    static {
        Thread asyncSetThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Triplet<NumericData<Number>, Number, MathOperator> data = asyncCalcQueue.take();
                        NumericData<Number> numericData = data.getValue0();
                        Number value = data.getValue1();
                        MathOperator operator = data.getValue2();
                        if (numericData != null) {
                            // Get a reference to the 'set' method
                            operator.apply(numericData, value);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }

                }
            }

        });

        asyncSetThread.start();

    }

    NumericData(@NotNull Bytes key, T defaultValue) {
        super(key, defaultValue);
    }

    public abstract T add(T value);

    public abstract T sub(T value);

    public abstract T mul(T value);

    public abstract T div(T value);

    public void addAsync(T value) {
        calcAsync(MathOperator.ADD, value);
    }

    public void subAsync(T value) {
        calcAsync(MathOperator.SUBTRACT, value);
    }

    public void mulAsync(T value) {
        calcAsync(MathOperator.MULTIPLY, value);
    }

    public void divAsync(T value) {
        calcAsync(MathOperator.DIVIDE, value);
    }

    @SuppressWarnings("unchecked")
    private void calcAsync(MathOperator operator, T value) {
        if (this.key == null) {
            printTempErrorMsg();
            return;
        }
        asyncCalcQueue.add(Triplet.with((NumericData<Number>) this, (Number) value, operator));
    }

}
