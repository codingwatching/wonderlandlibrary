/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector3f;

public class BlockPart {
    public final Vector3f positionFrom;
    public final Vector3f positionTo;
    public final Map<EnumFacing, BlockPartFace> mapFaces;
    public final BlockPartRotation partRotation;
    public final boolean shade;

    public BlockPart(Vector3f positionFromIn, Vector3f positionToIn, Map<EnumFacing, BlockPartFace> mapFacesIn, BlockPartRotation partRotationIn, boolean shadeIn) {
        this.positionFrom = positionFromIn;
        this.positionTo = positionToIn;
        this.mapFaces = mapFacesIn;
        this.partRotation = partRotationIn;
        this.shade = shadeIn;
        this.setDefaultUvs();
    }

    private void setDefaultUvs() {
        for (Map.Entry<EnumFacing, BlockPartFace> entry : this.mapFaces.entrySet()) {
            float[] afloat = this.getFaceUvs(entry.getKey());
            entry.getValue().blockFaceUV.setUvs(afloat);
        }
    }

    private float[] getFaceUvs(EnumFacing p_178236_1_) {
        float[] afloat;
        switch (p_178236_1_) {
            case DOWN: 
            case UP: {
                afloat = new float[]{this.positionFrom.x, this.positionFrom.z, this.positionTo.x, this.positionTo.z};
                break;
            }
            case NORTH: 
            case SOUTH: {
                afloat = new float[]{this.positionFrom.x, 16.0f - this.positionTo.y, this.positionTo.x, 16.0f - this.positionFrom.y};
                break;
            }
            case WEST: 
            case EAST: {
                afloat = new float[]{this.positionFrom.z, 16.0f - this.positionTo.y, this.positionTo.z, 16.0f - this.positionFrom.y};
                break;
            }
            default: {
                throw new NullPointerException();
            }
        }
        return afloat;
    }

    static class Deserializer
    implements JsonDeserializer<BlockPart> {
        Deserializer() {
        }

        @Override
        public BlockPart deserialize(JsonElement p_deserialize_1_, Type p_deserialize_2_, JsonDeserializationContext p_deserialize_3_) throws JsonParseException {
            JsonObject jsonobject = p_deserialize_1_.getAsJsonObject();
            Vector3f vector3f = this.parsePositionFrom(jsonobject);
            Vector3f vector3f1 = this.parsePositionTo(jsonobject);
            BlockPartRotation blockpartrotation = this.parseRotation(jsonobject);
            Map<EnumFacing, BlockPartFace> map = this.parseFacesCheck(p_deserialize_3_, jsonobject);
            if (jsonobject.has("shade") && !JsonUtils.isBoolean(jsonobject, "shade")) {
                throw new JsonParseException("Expected shade to be a Boolean");
            }
            boolean flag = JsonUtils.getBoolean(jsonobject, "shade", true);
            return new BlockPart(vector3f, vector3f1, map, blockpartrotation, flag);
        }

        private BlockPartRotation parseRotation(JsonObject p_178256_1_) {
            BlockPartRotation blockpartrotation = null;
            if (p_178256_1_.has("rotation")) {
                JsonObject jsonobject = JsonUtils.getJsonObject(p_178256_1_, "rotation");
                Vector3f vector3f = this.parsePosition(jsonobject, "origin");
                vector3f.scale(0.0625f);
                EnumFacing.Axis enumfacing$axis = this.parseAxis(jsonobject);
                float f2 = this.parseAngle(jsonobject);
                boolean flag = JsonUtils.getBoolean(jsonobject, "rescale", false);
                blockpartrotation = new BlockPartRotation(vector3f, enumfacing$axis, f2, flag);
            }
            return blockpartrotation;
        }

        private float parseAngle(JsonObject p_178255_1_) {
            float f2 = JsonUtils.getFloat(p_178255_1_, "angle");
            if (f2 != 0.0f && MathHelper.abs(f2) != 22.5f && MathHelper.abs(f2) != 45.0f) {
                throw new JsonParseException("Invalid rotation " + f2 + " found, only -45/-22.5/0/22.5/45 allowed");
            }
            return f2;
        }

        private EnumFacing.Axis parseAxis(JsonObject p_178252_1_) {
            String s2 = JsonUtils.getString(p_178252_1_, "axis");
            EnumFacing.Axis enumfacing$axis = EnumFacing.Axis.byName(s2.toLowerCase());
            if (enumfacing$axis == null) {
                throw new JsonParseException("Invalid rotation axis: " + s2);
            }
            return enumfacing$axis;
        }

        private Map<EnumFacing, BlockPartFace> parseFacesCheck(JsonDeserializationContext p_178250_1_, JsonObject p_178250_2_) {
            Map<EnumFacing, BlockPartFace> map = this.parseFaces(p_178250_1_, p_178250_2_);
            if (map.isEmpty()) {
                throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
            }
            return map;
        }

        private Map<EnumFacing, BlockPartFace> parseFaces(JsonDeserializationContext p_178253_1_, JsonObject p_178253_2_) {
            EnumMap<EnumFacing, BlockPartFace> map = Maps.newEnumMap(EnumFacing.class);
            JsonObject jsonobject = JsonUtils.getJsonObject(p_178253_2_, "faces");
            for (Map.Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                EnumFacing enumfacing = this.parseEnumFacing(entry.getKey());
                map.put(enumfacing, (BlockPartFace)p_178253_1_.deserialize(entry.getValue(), (Type)((Object)BlockPartFace.class)));
            }
            return map;
        }

        private EnumFacing parseEnumFacing(String name) {
            EnumFacing enumfacing = EnumFacing.byName(name);
            if (enumfacing == null) {
                throw new JsonParseException("Unknown facing: " + name);
            }
            return enumfacing;
        }

        private Vector3f parsePositionTo(JsonObject p_178247_1_) {
            Vector3f vector3f = this.parsePosition(p_178247_1_, "to");
            if (vector3f.x >= -16.0f && vector3f.y >= -16.0f && vector3f.z >= -16.0f && vector3f.x <= 32.0f && vector3f.y <= 32.0f && vector3f.z <= 32.0f) {
                return vector3f;
            }
            throw new JsonParseException("'to' specifier exceeds the allowed boundaries: " + vector3f);
        }

        private Vector3f parsePositionFrom(JsonObject p_178249_1_) {
            Vector3f vector3f = this.parsePosition(p_178249_1_, "from");
            if (vector3f.x >= -16.0f && vector3f.y >= -16.0f && vector3f.z >= -16.0f && vector3f.x <= 32.0f && vector3f.y <= 32.0f && vector3f.z <= 32.0f) {
                return vector3f;
            }
            throw new JsonParseException("'from' specifier exceeds the allowed boundaries: " + vector3f);
        }

        private Vector3f parsePosition(JsonObject p_178251_1_, String p_178251_2_) {
            JsonArray jsonarray = JsonUtils.getJsonArray(p_178251_1_, p_178251_2_);
            if (jsonarray.size() != 3) {
                throw new JsonParseException("Expected 3 " + p_178251_2_ + " values, found: " + jsonarray.size());
            }
            float[] afloat = new float[3];
            for (int i2 = 0; i2 < afloat.length; ++i2) {
                afloat[i2] = JsonUtils.getFloat(jsonarray.get(i2), p_178251_2_ + "[" + i2 + "]");
            }
            return new Vector3f(afloat[0], afloat[1], afloat[2]);
        }
    }
}

