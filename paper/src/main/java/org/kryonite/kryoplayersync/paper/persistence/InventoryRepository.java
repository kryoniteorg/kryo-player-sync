package org.kryonite.kryoplayersync.paper.persistence;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

  void save(UUID uniqueId, byte[] inventory) throws SQLException;

  void saveAll(Map<UUID , byte[]> inventories) throws SQLException;

  Optional<byte[]> get(UUID uniqueId) throws SQLException;
}
