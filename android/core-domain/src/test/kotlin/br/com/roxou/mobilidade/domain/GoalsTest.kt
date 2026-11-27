package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.goals.DailyGoalPlan
import br.com.roxou.mobilidade.domain.goals.GoalRedistribution
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoalsTest {
    private val plan = DailyGoalPlan.DEFAULT
    private val monday = LocalDate.of(2026, 11, 23)
    private val days = (0L..6L).map { monday.plusDays(it) }

    @Test fun `meta padrao soma 80`() {
        assertEquals(80, plan.weeklyTotal)
    }

    @Test fun `sem corridas a meta fica como planejada`() {
        val s = GoalRedistribution.compute(plan, days, emptyMap(), today = monday)
        assertEquals(listOf(8, 6, 6, 6, 18, 20, 16), s.days.map { it.adjustedTarget })
        assertEquals(80, s.remaining)
    }

    @Test fun `excedente de um dia reduz a meta dos dias seguintes`() {
        // Segunda fez 14 (meta 8): 6 a mais. Restam 66 para ter-dom, que somavam 72.
        val s = GoalRedistribution.compute(plan, days, mapOf(monday to 14), today = monday.plusDays(1))
        val open = s.days.drop(1).map { it.adjustedTarget }
        assertEquals(66, open.sum())
        assertEquals(8, s.days[0].adjustedTarget) // passado não muda
        open.zip(listOf(6, 6, 6, 18, 20, 16)).forEach { (adj, orig) -> assert(adj <= orig) }
    }

    @Test fun `atraso nao aumenta os dias seguintes por padrao`() {
        val s = GoalRedistribution.compute(plan, days, mapOf(monday to 2), today = monday.plusDays(1))
        assertEquals(listOf(6, 6, 6, 18, 20, 16), s.days.drop(1).map { it.adjustedTarget })
        assertEquals(78, s.remaining)
    }

    @Test fun `atraso pode ser redistribuido quando configurado`() {
        val s = GoalRedistribution.compute(plan, days, mapOf(monday to 2), today = monday.plusDays(1), carryShortfall = true)
        assertEquals(78, s.days.drop(1).sumOf { it.adjustedTarget })
    }

    @Test fun `meta semanal batida zera os dias restantes`() {
        val done = mapOf(monday to 30, monday.plusDays(1) to 30, monday.plusDays(2) to 25)
        val s = GoalRedistribution.compute(plan, days, done, today = monday.plusDays(3))
        assertEquals(0, s.days.drop(3).sumOf { it.adjustedTarget })
        assertEquals(0, s.remaining)
    }

    @Test fun `hoje mostra quanto falta no dia`() {
        val s = GoalRedistribution.compute(plan, days, mapOf(monday.plusDays(4) to 11), today = monday.plusDays(4))
        assertEquals(18, s.today?.adjustedTarget)
        assertEquals(7, s.today?.remaining)
    }

    @Test fun `distribuicao preserva o total com numeros inteiros`() {
        assertEquals(listOf(1, 1, 1), GoalRedistribution.distribute(3, listOf(1, 1, 1)))
        assertEquals(10, GoalRedistribution.distribute(10, listOf(3, 3, 3)).sum())
        assertEquals(listOf(2, 1), GoalRedistribution.distribute(3, listOf(0, 0)))
    }
}
