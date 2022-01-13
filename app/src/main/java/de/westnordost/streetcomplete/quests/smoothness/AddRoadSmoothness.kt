package de.westnordost.streetcomplete.quests.smoothness

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.meta.deleteCheckDatesForKey
import de.westnordost.streetcomplete.data.meta.updateWithCheckDate
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.user.achievements.QuestTypeAchievement.CAR
import de.westnordost.streetcomplete.data.user.achievements.QuestTypeAchievement.BICYCLIST
import de.westnordost.streetcomplete.ktx.arrayOfNotNull

class AddRoadSmoothness : OsmFilterQuestType<SmoothnessAnswer>() {

    override val elementFilter = """
        ways with (
            highway ~ ${ROADS_TO_ASK_SMOOTHNESS_FOR.joinToString("|")}
            or highway = service and service !~ driveway|slipway
          )
          and surface ~ ${SURFACES_FOR_SMOOTHNESS.joinToString("|")}
          and (access !~ private|no or (foot and foot !~ private|no))
          and (
            !smoothness
            or smoothness older today -6 years
            or smoothness:date < today -6 years
          )
    """

    override val commitMessage = "Add road smoothness"
    override val wikiLink = "Key:smoothness"
    override val icon = R.drawable.ic_quest_street_surface_detail
    override val isSplitWayEnabled = true
    override val questTypeAchievements = listOf(CAR, BICYCLIST)

    override fun getTitle(tags: Map<String, String>): Int {
        val hasName = tags.containsKey("name")
        val isSquare = tags["area"] == "yes"
        return when {
            hasName ->     R.string.quest_smoothness_name_title
            isSquare ->    R.string.quest_smoothness_square_title
            else ->        R.string.quest_smoothness_road_title
        }
    }

    override fun getTitleArgs(tags: Map<String, String>, featureName: Lazy<String?>): Array<String> =
        arrayOfNotNull(tags["name"])

    override fun createForm() = AddSmoothnessForm()

    override fun applyAnswerTo(answer: SmoothnessAnswer, changes: StringMapChangesBuilder) {
        when (answer) {
            is SmoothnessValueAnswer -> {
                changes.updateWithCheckDate("smoothness", answer.value.osmValue)
                changes.deleteIfExists("smoothness:date")
            }
            is WrongSurfaceAnswer -> {
                changes.delete("surface")
                changes.deleteIfExists("smoothness")
                changes.deleteIfExists("smoothness:date")
                changes.deleteCheckDatesForKey("smoothness")
            }
            is IsActuallyStepsAnswer -> throw IllegalStateException()
        }
    }
}

// surfaces that are actually used in AddSmoothnessForm
// should only contain values that are in the Surface class
val SURFACES_FOR_SMOOTHNESS = listOf(
    "asphalt", "concrete", "concrete:plates", "sett", "paving_stones", "compacted", "gravel", "fine_gravel"
)

private val ROADS_TO_ASK_SMOOTHNESS_FOR = arrayOf(
    // "trunk","trunk_link","motorway","motorway_link", // too much, motorways are almost by definition smooth asphalt (or concrete)
    "primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link",
    "unclassified", "residential", "living_street", "pedestrian", "track",
    // "service", // this is too much, and the information value is very low
)