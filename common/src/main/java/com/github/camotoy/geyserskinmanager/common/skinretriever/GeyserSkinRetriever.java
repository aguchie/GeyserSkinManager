package com.github.camotoy.geyserskinmanager.common.skinretriever;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.camotoy.geyserskinmanager.common.RawCape;
import com.github.camotoy.geyserskinmanager.common.RawSkin;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.BedrockClientData;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class GeyserSkinRetriever implements BedrockSkinRetriever {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public RawCape getBedrockCape(UUID uuid) {
        GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
        if (session == null) {
            return null;
        }

        if (session.getClientData().getCapeImageWidth() == 0 || session.getClientData().getCapeImageHeight() == 0 ||
                session.getClientData().getCapeData().length == 0) {
            return null;
        }
        return new RawCape(session.getClientData().getCapeImageWidth(), session.getClientData().getCapeImageHeight(),
                session.getClientData().getCapeId(), session.getClientData().getCapeData());
    }

    @Override
    public RawSkin getBedrockSkin(String name) {
        GeyserSession session = null;
        for (GeyserSession otherSession : GeyserImpl.getInstance().getSessionManager().getSessions().values()) {
            if (name.equals(otherSession.name())) {
                session = otherSession;
                break;
            }
        }
        if (session == null) {
            return null;
        }

        return getImage(session.getClientData());
    }

    @Override
    public RawSkin getBedrockSkin(UUID uuid) {
        GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
        if (session == null) {
            return null;
        }

        return getImage(session.getClientData());
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return GeyserImpl.getInstance().connectionByUuid(uuid) != null;
    }

    /**
     * Taken from https://github.com/NukkitX/Nukkit/blob/master/src/main/java/cn/nukkit/network/protocol/LoginPacket.java
     */
    private RawSkin getImage(BedrockClientData clientData) {
        byte[] image = getSkinData(clientData);
        if (image == null || image.length > (128 * 128 * 4) || clientData.isPersonaSkin()) {
            return null;
        }
        String geometryName = getGeometryName(clientData);
        boolean alex = isAlex(geometryName);
        return new RawSkin(
                clientData.getSkinImageWidth(),
                clientData.getSkinImageHeight(),
                image, alex, geometryName,
                getGeometryData(clientData),
                getRawSkinData(clientData)
        );
    }

    private byte[] getSkinData(BedrockClientData clientData) {
        try {
            Object skinData = clientData.getClass().getMethod("getSkinData").invoke(clientData);
            if (skinData instanceof String) {
                return Base64.getDecoder().decode((String) skinData);
            } else if (skinData instanceof byte[]) {
                return (byte[]) skinData;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRawSkinData(BedrockClientData clientData) {
        try {
            Object skinData = clientData.getClass().getMethod("getSkinData").invoke(clientData);
            if (skinData instanceof String) {
                return (String) skinData;
            } else if (skinData instanceof byte[]) {
                return Base64.getEncoder().encodeToString((byte[]) skinData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getGeometryName(BedrockClientData clientData) {
        try {
            Object geometryName = clientData.getClass().getMethod("getGeometryName").invoke(clientData);
            if (geometryName instanceof String) {
                return new String(Base64.getDecoder().decode((String) geometryName), StandardCharsets.UTF_8);
            } else if (geometryName instanceof byte[]) {
                return new String((byte[]) geometryName, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getGeometryData(BedrockClientData clientData) {
        try {
            Object geometryData = clientData.getClass().getMethod("getGeometryData").invoke(clientData);
            if (geometryData instanceof String) {
                return new String(Base64.getDecoder().decode((String) geometryData), StandardCharsets.UTF_8);
            } else if (geometryData instanceof byte[]) {
                return new String((byte[]) geometryData, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean isAlex(String geometryName) {
        try {
            String defaultGeometryName = OBJECT_MAPPER.readTree(geometryName).get("geometry").get("default").asText();
            return "geometry.humanoid.customSlim".equals(defaultGeometryName);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
