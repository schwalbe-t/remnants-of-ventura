
package schwalbe.ventura.server.game

import schwalbe.ventura.bigton.BigtonFeature
import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.data.ItemType


class ProcessorFeatures(
    val features: Set<BigtonFeature>,
    val modules: List<BigtonModule<World>>,
    val supportedAttachments: Set<ItemType>
)

val BIGTON_1000_FEATURES = ProcessorFeatures(
    features = setOf(),
    modules = listOf(
        BIGTON_MODULES.standard
    ),
    supportedAttachments = setOf()
)
val BIGTON_2000_FEATURES = ProcessorFeatures(
    features = setOf(
        BigtonFeature.DYNAMIC_MEMORY,
        BigtonFeature.CUSTOM_FUNCTIONS
    ),
    modules = listOf(
        BIGTON_MODULES.standard
    ),
    supportedAttachments = setOf(
        ItemType.PIVOTAL_ME2048,
        ItemType.PIVOTAL_ME5120,
        ItemType.PIVOTAL_ME10K,
        ItemType.PIVOTAL_ME20K
    )
)
val BIGTON_3000_FEATURES = ProcessorFeatures(
    features = setOf(
        BigtonFeature.DYNAMIC_MEMORY,
        BigtonFeature.CUSTOM_FUNCTIONS,
        BigtonFeature.FPU_MODULE
    ),
    modules = listOf(
        BIGTON_MODULES.standard,
        BIGTON_MODULES.floatingPoint
    ),
    supportedAttachments = setOf(
        ItemType.PIVOTAL_ME2048,
        ItemType.PIVOTAL_ME5120,
        ItemType.PIVOTAL_ME10K,
        ItemType.PIVOTAL_ME20K
    )
)


class ProcessorStats(
    val baseMemory: Long,
    val instructionLimit: Long,
    val maxCallDepth: Int,
    val maxTupleSize: Int
)

val BIGTON_STATS_0 = ProcessorStats(
    baseMemory = 1.0.kb,
    instructionLimit = 1000,
    maxCallDepth = 32,
    maxTupleSize = 4
)
val BIGTON_STATS_1 = ProcessorStats(
    baseMemory = 1.5.kb,
    instructionLimit = 2000,
    maxCallDepth = 64,
    maxTupleSize = 6
)
val BIGTON_STATS_2 = ProcessorStats(
    baseMemory = 2.0.kb,
    instructionLimit = 3000,
    maxCallDepth = 128,
    maxTupleSize = 8
)
val BIGTON_STATS_3 = ProcessorStats(
    baseMemory = 2.5.kb,
    instructionLimit = 4000,
    maxCallDepth = 256,
    maxTupleSize = 12
)
val BIGTON_STATS_4 = ProcessorStats(
    baseMemory = 3.0.kb,
    instructionLimit = 5000,
    maxCallDepth = 512,
    maxTupleSize = 16
)


class ProcessorInfo(
    val features: ProcessorFeatures,
    val stats: ProcessorStats
)

val PROCESSOR_INFO: Map<ItemType, ProcessorInfo> = mapOf(
    ItemType.BIGTON_1030 to ProcessorInfo(BIGTON_1000_FEATURES, BIGTON_STATS_0),
    ItemType.BIGTON_1050 to ProcessorInfo(BIGTON_1000_FEATURES, BIGTON_STATS_1),
    ItemType.BIGTON_1070 to ProcessorInfo(BIGTON_1000_FEATURES, BIGTON_STATS_2),

    ItemType.BIGTON_2030 to ProcessorInfo(BIGTON_2000_FEATURES, BIGTON_STATS_1),
    ItemType.BIGTON_2050 to ProcessorInfo(BIGTON_2000_FEATURES, BIGTON_STATS_2),
    ItemType.BIGTON_2070 to ProcessorInfo(BIGTON_2000_FEATURES, BIGTON_STATS_3),

    ItemType.BIGTON_3030 to ProcessorInfo(BIGTON_3000_FEATURES, BIGTON_STATS_2),
    ItemType.BIGTON_3050 to ProcessorInfo(BIGTON_3000_FEATURES, BIGTON_STATS_3),
    ItemType.BIGTON_3070 to ProcessorInfo(BIGTON_3000_FEATURES, BIGTON_STATS_4)
)