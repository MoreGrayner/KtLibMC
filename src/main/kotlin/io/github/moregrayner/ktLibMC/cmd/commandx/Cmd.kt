package io.github.moregrayner.ktLibMC.cmd.annotations

// 명령어 발신자 유형
enum class SenderType {
    PLAYER, CONSOLE, BOTH
}

// 명령어 실행을 위한 인터페이스
interface ICommandExecutor {
    fun execute(args: List<String>, sender: CommandSender)
}

// 명령어를 실행할 주체 (플레이어, 콘솔, 봇)
open class CommandSender
class Player(val isOp: Boolean) : CommandSender()
class Console : CommandSender()
class Bot : CommandSender()

// 명령어를 관리하는 클래스
class CommandNode(
    val name: String,
    val executor: ICommandExecutor
) {
    private val subCommands = mutableMapOf<String, CommandNode>()

    // 하위 명령어 등록
    fun then(subCommand: String, action: (args: List<String>, sender: CommandSender) -> Unit): CommandNode {
        val newNode = CommandNode(subCommand, object : ICommandExecutor {
            override fun execute(args: List<String>, sender: CommandSender) {
                action(args, sender)
            }
        })
        subCommands[subCommand] = newNode
        return newNode
    }

    // 최하위 명령어 등록 (then과 동일하지만 구분용)
    fun last(subCommand: String, action: (args: List<String>, sender: CommandSender) -> Unit): CommandNode {
        return then(subCommand, action)
    }

    // 명령어 실행
    fun execute(args: List<String>, sender: CommandSender) {
        if (args.isNotEmpty()) {
            val subCommand = subCommands[args[0]]
            subCommand?.execute(args.drop(1), sender)
        } else {
            executor.execute(args, sender)
        }
    }
}

// 명령어 등록 및 실행을 관리하는 싱글톤
object CommandManager {
    private val commands = mutableMapOf<String, CommandNode>()

    fun registerCommand(name: String, executor: ICommandExecutor): CommandNode {
        val node = CommandNode(name, executor)
        commands[name] = node
        return node
    }

    fun executeCommand(commandString: String, sender: CommandSender) {
        val parts = commandString.split(" ")
        commands[parts[0]]?.execute(parts.drop(1), sender)
    }
}

// 명령어 등록을 위한 확장 함수
fun CommandGroup.register(name: String, action: CommandNode.() -> Unit) {
    val rootNode = CommandManager.registerCommand(name, object : ICommandExecutor {
        override fun execute(args: List<String>, sender: CommandSender) {
            // 루트 노드는 실행되지 않음 (하위 명령어 실행을 위함)
        }
    })
    rootNode.action()
}

// OP 전용 명령어 그룹
object op : CommandGroup()
object other : CommandGroup()

// 명령어 그룹 인터페이스
open class CommandGroup {
    fun register(name: String, action: CommandNode.() -> Unit) {
        val rootNode = CommandManager.registerCommand(name, object : ICommandExecutor {
            override fun execute(args: List<String>, sender: CommandSender) {
                // 실행 로직
            }
        })
        rootNode.action()
    }
}

// @Register 어노테이션
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Register(val sender: SenderType)

