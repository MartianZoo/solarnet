package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.search.CanonQuery.Form

internal val queryExamples =
    listOf(
        QueryExample(
            "Finding cards",
            "Which Space cards cost at most 15 MC?",
            "/Canon/Card[@cost <= 15 and tags/Tag/@name='SpaceTag']",
        ),
        QueryExample(
            "Finding cards",
            "Which cards have both Building and Power tags?",
            "/Canon/Card[tags/Tag/@name='BuildingTag' and tags/Tag/@name='PowerTag']",
        ),
        QueryExample(
            "Finding cards",
            "Which Plant cards have no Building tag?",
            "/Canon/Card[tags/Tag/@name='PlantTag' and not(tags/Tag/@name='BuildingTag')]",
        ),
        QueryExample(
            "Finding cards",
            "Which cards have at least 3 tags, counting repeated tags?",
            "/Canon/Card[sum(tags/Tag/@count) >= 3]",
        ),
        QueryExample(
            "Finding cards",
            "Which cards repeat the same tag?",
            "/Canon/Card[tags/Tag[@count > 1]]",
        ),
        QueryExample(
            "Finding cards",
            "Which project cards have no tags except possibly the event icon?",
            "/Canon/Card[@deck='ProjectCard' and not(tags/Tag[@name!='EventTag'])]",
        ),
        QueryExample(
            "Finding cards",
            "Which Preludes have a Science tag?",
            "/Canon/Card[@deck='PreludeCard' and tags/Tag/@name='ScienceTag']",
        ),
        QueryExample(
            "Finding cards",
            "Which corporations hold resources on their cards?",
            "/Canon/Card[@deck='CorporationCard' and @resource]",
        ),
        QueryExample(
            "Finding cards",
            "Which blue project cards cost no more than 10 MC?",
            "/Canon/Card[@deck='ProjectCard' and @category='ActiveCard' and @cost <= 10]",
        ),
        QueryExample(
            "Finding cards",
            "Which card names contain asteroid, ignoring capitalization?",
            "/Canon/Card[contains(translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'asteroid')]",
        ),
        QueryExample(
            "Playing cards",
            "Which cards have a play effect that gains 2 or more plant production, even inside a choice?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//Gain[@count >= 2 and @gainingClass='Production'
                and .//Expression[@className='Plant']]]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Playing cards",
            "Which cards have a play effect that gains plants without gaining plant production?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@gainingClass='Plant']
              and not(effects/Effect[trigger/*[@selfGainOnly='true']]
                /instruction//*[@gainingClass='Production'][.//Expression[@className='Plant']])]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Playing cards",
            "Which cards have a play effect that draws at least 2 project cards at a time?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@gainingClass='ProjectCard' and @count >= 2]]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Playing cards",
            "Which cards have both TR gains and production losses in their play effects?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@gainingClass='TerraformRating']
              and effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@removingClass='Production']]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Playing cards",
            "Which cards have a play effect that places a tile of any kind?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@gainingClass][id(@gainingClass)/isA/Type/@name='Tile']]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Playing cards",
            "Which cards mention both a temperature increase and an ocean placement in their play effects?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@gainingClass='TemperatureStep']
              and effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@gainingClass='OceanTile']]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Playing cards",
            "Which cards have a play effect that removes plant production?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//*[@removingClass='Production'][.//Expression[@className='Plant']]]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Playing cards",
            "Which cards add resources specifically to themselves when played?",
            """
            /Canon/Card[effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//Gain[id(@gainingClass)/isA/Type/@name='Resource']
                [scaledEx/ScaledExpression/expression/Expression/arguments/Expression[@className='This']]]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Requirements and conditions",
            "Which project cards have no printed play requirement?",
            "/Canon/Card[@deck='ProjectCard' and not(requirement)]",
        ),
        QueryExample(
            "Requirements and conditions",
            "Which cards have a printed Science-tag requirement?",
            "/Canon/Card[requirement//Count[@countedClass='ScienceTag']]",
        ),
        QueryExample(
            "Requirements and conditions",
            "Which cards require oxygen to be no higher than a limit?",
            "/Canon/Card[requirement//Max[countedMetric/Count/@countedClass='OxygenStep']]",
        ),
        QueryExample(
            "Requirements and conditions",
            "Which cards require at least 3 oceans?",
            "/Canon/Card[requirement//Min[@minimum >= 3 and countedMetric/Count/@countedClass='OceanTile']]",
        ),
        QueryExample(
            "Requirements and conditions",
            "Which cards check Plant tags inside an effect rather than in their play requirement?",
            """
            /Canon/Card[effects/Effect/instruction//Gated/gate//Count[@countedClass='PlantTag']
              and not(requirement//Count[@countedClass='PlantTag'])]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Actions and choices",
            "Which cards have an action with no explicit cost?",
            "/Canon/Card[actions/Action[not(cost)]]",
        ),
        QueryExample(
            "Actions and choices",
            "Which cards have an action that spends microbes?",
            "/Canon/Card[actions/Action/cost//Spend/scaledEx/ScaledExpression/expression/Expression[@className='Microbe']]",
        ),
        QueryExample(
            "Actions and choices",
            "Which cards have an action that spends energy to draw project cards?",
            """
            /Canon/Card[actions/Action[
              cost//Spend/scaledEx/ScaledExpression/expression/Expression[@className='Energy']
              and instruction//*[@gainingClass='ProjectCard']]]
            """
                .trimIndent(),
        ),
        QueryExample(
            "Actions and choices",
            "Which cards have at least 2 separately declared actions?",
            "/Canon/Card[count(actions/Action) >= 2]",
        ),
        QueryExample(
            "Actions and choices",
            "Which cards let an action choose between alternatives?",
            "/Canon/Card[actions/Action/instruction//Or]",
        ),
        QueryExample(
            "Actions and choices",
            "Which cards use an X-sized gain or removal in an action?",
            "/Canon/Card[actions/Action/instruction//*[@xMultiple]]",
        ),
        QueryExample(
            "Actions and choices",
            "Which cards scale an effect by the number of Building tags?",
            "/Canon/Card[effects/Effect/instruction//Per/metric//Count[@countedClass='BuildingTag']]",
        ),
        QueryExample(
            "Actions and choices",
            "Which cards scale a production gain by a counted quantity?",
            """
            /Canon/Card[effects/Effect/instruction//Per[
              inner//*[@gainingClass='Production'] and metric//Count]]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Ongoing effects",
            "Which cards react to a city being placed?",
            "/Canon/Card[effects/Effect/trigger//OnGainOf/expression/Expression[@className='CityTile']]",
        ),
        QueryExample(
            "Ongoing effects",
            "Which cards have a triggered effect with an additional IF condition?",
            "/Canon/Card[effects/Effect/trigger//IfTrigger]",
        ),
        QueryExample(
            "Ongoing effects",
            "Which cards react to any biological tag, including the shared BioTag type?",
            """
            /Canon/Card[effects/Effect/trigger//OnGainOf/expression/Expression[
              id(@className)/isA/Type/@name='BioTag']]
            """
                .trimIndent(),
        ),
        QueryExample(
            "Ongoing effects",
            "Which cards reduce what is owed while paying for something?",
            """
            /Canon/Card[effects/Effect[
              trigger//OnGainOf/expression/Expression[@className='PayingFor']
              and instruction//*[@removingClass='Owed']]]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Ongoing effects",
            "Which cards declare automatic effects?",
            "/Canon/Card[effects/Effect[@automatic='true']]",
        ),
        QueryExample(
            "Scoring and stored resources",
            "Which cards can hold animals?",
            "/Canon/Card[@resource='Animal']",
        ),
        QueryExample(
            "Scoring and stored resources",
            "Which cards score at game end according to their animals?",
            """
            /Canon/Card[effects/Effect[trigger/*[@endOnly='true']]
              /instruction//Per[inner//*[@gainingClass='VictoryPoint']
                and metric//Count[@countedClass='Animal']]]
            """
                .trimIndent(),
        ),
        QueryExample(
            "Scoring and stored resources",
            "Which cards give at least 2 fixed VP in a single end-game instruction?",
            """
            /Canon/Card[effects/Effect[trigger/*[@endOnly='true']]
              /instruction/Gain[@gainingClass='VictoryPoint' and @count >= 2]]
            """
                .trimIndent(),
        ),
        QueryExample(
            "Scoring and stored resources",
            "Which cards lose VP at game end?",
            """
            /Canon/Card[effects/Effect[trigger/*[@endOnly='true']]
              /instruction//*[@removingClass='VictoryPoint']]
            """
                .trimIndent(),
        ),
        QueryExample(
            "Exploring the model",
            "What are all the concrete standard projects?",
            "/Canon/Class[@abstract='false' and isA/Type/@name='StandardProject']",
        ),
        QueryExample(
            "Exploring the model",
            "What are all the concrete milestones and awards?",
            "/Canon/Class[@abstract='false'][isA/Type/@name='Milestone' or isA/Type/@name='Award']",
        ),
        QueryExample(
            "Exploring the model",
            "Which classes directly inherit from Tile?",
            "/Canon/Class[supertypes/Expression/@className='Tile']",
        ),
        QueryExample(
            "Exploring the model",
            "Which cards mention Venus anywhere in an expression?",
            "/Canon/Card[.//Expression[@className='VenusStep']]",
        ),
        QueryExample(
            "Exploring the model",
            "Which classes declare a positive exact number of Building tags attached to This?",
            """
            /Canon/*[invariants//Exact[@target > 0]/countedMetric/Count[
              @countedClass='BuildingTag'
              and expression/Expression/arguments/Expression[@className='This']]]
            """
                .trimIndent(),
        ),
        QueryExample(
            "Exploring the model",
            "Which cards still spell their production changes using PROD?",
            "/Canon/Card[.//Transform[@transformKind='PROD']]",
        ),
        QueryExample(
            "Returning details and totals",
            "Show the actual 2-or-more plant-production instructions and the cards containing them.",
            """
            /Canon/Card/effects/Effect[trigger/*[@selfGainOnly='true']]
              /instruction//Gain[@count >= 2 and @gainingClass='Production'
                and .//Expression[@className='Plant']]
            """
                .trimIndent(),
            Form.ELABORATED,
        ),
        QueryExample(
            "Returning details and totals",
            "Show the printed requirements of cards that have them.",
            "/Canon/Card/requirement/*",
        ),
        QueryExample(
            "Returning details and totals",
            "Show every declared card action, including its cost and result.",
            "/Canon/Card/actions/Action",
        ),
        QueryExample(
            "Returning details and totals",
            "Show the prices of Space cards alongside their names.",
            "/Canon/Card[tags/Tag/@name='SpaceTag']/@cost",
        ),
        QueryExample(
            "Returning details and totals",
            "How many project cards have a Science tag?",
            "count(/Canon/Card[@deck='ProjectCard' and tags/Tag/@name='ScienceTag'])",
        ),
        QueryExample(
            "Returning details and totals",
            "What is the combined printed price of all Space events?",
            "sum(/Canon/Card[@category='EventCard' and tags/Tag/@name='SpaceTag']/@cost)",
        ),
        QueryExample(
            "Returning details and totals",
            "Are there any cards with more than 3 copies of a tag?",
            "boolean(/Canon/Card[tags/Tag[@count > 3]])",
        ),
    )
