package io.github.moregrayner.ktLibMC

package io.github.moregrayner.ktLibMC

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.*


class KtLibMC(private val plugin: Plugin, title: String, size: Int) : InventoryHolder, Listener {
    private val inventory = Bukkit.createInventory(this, size, title)
    private val actions: MutableMap<Int, Runnable> = HashMap()
    private val movableItems: MutableMap<Int, Boolean> = HashMap()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        guiMap[title] = this

        // 아이템이 설정되지 않은 슬롯을 배리어로 채움
        fillEmptySlotsWithBarrier()
    }

    override fun getInventory(): Inventory {
        return inventory
    }

    // 특정 슬롯에 아이템 배치
    fun setItem(slot: Int, material: Material?, amount: Int, movable: Boolean, action: Runnable) {
        if (slot >= 0 && slot < inventory.size && amount > 0) {
            val item = ItemStack(material!!, amount)
            inventory.setItem(slot, item)
            actions[slot] = action
            movableItems[slot] = movable
        }

        // 아이템이 설정된 후, 배리어로 채워진 슬롯들을 업데이트
        fillEmptySlotsWithBarrier()
    }

    // 아이템이 배정되지 않은 슬롯을 배리어로 채우기
    private fun fillEmptySlotsWithBarrier() {
        for (i in 0 until inventory.size) {
            if (inventory.getItem(i) == null) {
                val barrier = ItemStack(Material.BARRIER, 1)
                inventory.setItem(i, barrier)
                movableItems[i] = false // 배리어는 이동 불가능
            }
        }
    }

    // GUI 열기
    fun open(player: Player) {
        player.openInventory(inventory)
    }

    // 개별 플레이어 창고 열기
    fun openStorage(player: Player) {
        val uuid = player.uniqueId
        val storage = Bukkit.createInventory(player, 27, "개인 창고")

        // 저장된 데이터 불러오기
        if (storageData.containsKey(uuid)) {
            val items = storageData[uuid]!!
            storage.contents = items
        }

        player.openInventory(storage)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory.holder is KtLibMC) {
            val slot = event.rawSlot
            val gui = event.inventory.holder as KtLibMC?

            // 배리어 아이템 클릭 시 이동 불가
            if (!gui!!.movableItems.getOrDefault(slot, true)) {
                event.isCancelled = true
            }

            // 클릭 이벤트 실행
            if (gui.actions.containsKey(slot)) {
                gui.actions[slot]!!.run()
            }
        }
    }

    // 창고 닫을 때 아이템 저장
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.view.title == "개인 창고") {
            val player = event.player as Player
            val uuid = player.uniqueId
            storageData[uuid] = event.inventory.contents
        }
    }

    companion object {
        private val guiMap: MutableMap<String, CustomGUI> = HashMap()
        private val storageData: MutableMap<UUID, Array<ItemStack>> = HashMap() // 플레이어 개별 창고 데이터

        // 특정 이름의 GUI 가져오기
        fun getGUI(title: String): CustomGUI? {
            return guiMap[title]
        }
    }
}
