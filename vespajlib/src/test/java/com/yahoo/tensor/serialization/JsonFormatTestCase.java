// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.tensor.serialization;

import com.yahoo.tensor.Tensor;
import com.yahoo.tensor.TensorType;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author bratseth
 */
public class JsonFormatTestCase {

    @Test
    public void testSparseTensor() {
        Tensor.Builder builder = Tensor.Builder.of(TensorType.fromSpec("tensor(x{},y{})"));
        builder.cell().label("x", "a").label("y", "b").value(2.0);
        builder.cell().label("x", "c").label("y", "d").value(3.0);
        Tensor tensor = builder.build();
        byte[] json = JsonFormat.encode(tensor);
        assertEquals("{\"cells\":[" +
                     "{\"address\":{\"x\":\"a\",\"y\":\"b\"},\"value\":2.0}," +
                     "{\"address\":{\"x\":\"c\",\"y\":\"d\"},\"value\":3.0}" +
                     "]}",
                     new String(json, StandardCharsets.UTF_8));
        Tensor decoded = JsonFormat.decode(tensor.type(), json);
        assertEquals(tensor, decoded);
    }

    @Test
    public void testDenseTensor() {
        Tensor.Builder builder = Tensor.Builder.of(TensorType.fromSpec("tensor(x[2],y[2])"));
        builder.cell().label("x", 0).label("y", 0).value(2.0);
        builder.cell().label("x", 0).label("y", 1).value(3.0);
        builder.cell().label("x", 1).label("y", 0).value(5.0);
        builder.cell().label("x", 1).label("y", 1).value(7.0);
        Tensor tensor = builder.build();
        byte[] json = JsonFormat.encode(tensor);
        assertEquals("{\"cells\":[" +
                     "{\"address\":{\"x\":\"0\",\"y\":\"0\"},\"value\":2.0}," +
                     "{\"address\":{\"x\":\"0\",\"y\":\"1\"},\"value\":3.0}," +
                     "{\"address\":{\"x\":\"1\",\"y\":\"0\"},\"value\":5.0}," +
                     "{\"address\":{\"x\":\"1\",\"y\":\"1\"},\"value\":7.0}" +
                     "]}",
                     new String(json, StandardCharsets.UTF_8));
        Tensor decoded = JsonFormat.decode(tensor.type(), json);
        assertEquals(tensor, decoded);
    }

    @Test
    public void testMixedTensor() {
        Tensor.Builder builder = Tensor.Builder.of(TensorType.fromSpec("tensor(x{},y[2])"));
        builder.cell().label("x", "a").label("y", "0").value(1.0);
        builder.cell().label("x", "a").label("y", "1").value(2.0);
        builder.cell().label("x", "b").label("y", "0").value(3.0);
        builder.cell().label("x", "b").label("y", "1").value(4.0);
        Tensor tensor = builder.build();
        byte[] json = JsonFormat.encode(tensor);
        assertEquals("{\"cells\":[" +
                     "{\"address\":{\"x\":\"a\"},\"values\":[1.0,2.0]}," +
                     "{\"address\":{\"x\":\"b\"},\"values\":[3.0,4.0]}" +
                     "]}",
                     new String(json, StandardCharsets.UTF_8));
        Tensor decoded = JsonFormat.decode(tensor.type(), json);
        assertEquals(tensor, decoded);
    }

    @Test
    public void testDenseTensorInDenseForm() {
        Tensor.Builder builder = Tensor.Builder.of(TensorType.fromSpec("tensor(x[2],y[3])"));
        builder.cell().label("x", 0).label("y", 0).value(2.0);
        builder.cell().label("x", 0).label("y", 1).value(3.0);
        builder.cell().label("x", 0).label("y", 2).value(4.0);
        builder.cell().label("x", 1).label("y", 0).value(5.0);
        builder.cell().label("x", 1).label("y", 1).value(6.0);
        builder.cell().label("x", 1).label("y", 2).value(7.0);
        Tensor expected = builder.build();
        String denseJson = "{\"values\":[2.0, 3.0, 4.0, 5.0, 6.0, 7.0]}";
        Tensor decoded = JsonFormat.decode(expected.type(), denseJson.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, decoded);
    }

    @Test
    public void testTooManyCells() {
        TensorType x2 = TensorType.fromSpec("tensor(x[2])");
        String json = "{\"cells\":[" +
                      "{\"address\":{\"x\":\"0\"},\"value\":2.0}," +
                      "{\"address\":{\"x\":\"1\"},\"value\":3.0}," +
                      "{\"address\":{\"x\":\"2\"},\"value\":5.0}" +
                      "]}";
        try {
            JsonFormat.decode(x2, json.getBytes(StandardCharsets.UTF_8));
            fail("Excpected exception");
        }
        catch (IllegalArgumentException e) {
            assertEquals("cell address (2) is not within the bounds of tensor(x[2])", e.getMessage());
        }
    }

}
