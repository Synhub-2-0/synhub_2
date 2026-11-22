package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Plan del usuario: consultar y cambiar (pago simulado), y su efecto en los grupos que lidera. */
class SubscriptionApiTests extends ApiTestSupport {

    private boolean locked(long groupId, Account who) {
        return get("/groups/" + groupId, who.token()).json().get("locked").asBoolean();
    }

    private Res changePlan(Account who, String plan, List<Long> activeGroupIds) {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("plan", plan);
        if (activeGroupIds != null) body.put("activeGroupIds", activeGroupIds);
        return put("/subscriptions/me", who.token(), body);
    }

    @Test
    void everyoneStartsOnTheFreePlanAndSeesTheCatalog() {
        var user = account("usuario");

        var me = get("/subscriptions/me", user.token());

        assertEquals(200, me.status(), me.raw());
        assertEquals("FREE", me.text("plan"));
        assertEquals("Gratis", me.text("planName"));
        assertEquals(2, me.json().get("maxLedGroups").asInt());
        assertEquals(0, me.json().get("activeLedGroups").asInt());
        assertEquals(2, me.json().get("availableSlots").asInt());
        var plans = me.json().get("plans");
        assertEquals(3, plans.size());
        assertEquals("PRO", plans.get(1).get("plan").asString());
        assertEquals(9, plans.get(1).get("monthlyPrice").asInt());
        assertEquals(5, plans.get(1).get("maxLedGroups").asInt());
        assertTrue(plans.get(2).get("maxLedGroups").isNull(), "Ultra no tiene límite");
    }

    @Test
    void theSubscriptionNeedsASession() {
        assertEquals(401, get("/subscriptions/me", null).status());
        assertEquals(401, put("/subscriptions/me", null, Map.of("plan", "PRO")).status());
    }

    @Test
    void upgradingRaisesTheLimitButDoesNotActivateLockedGroupsByItself() {
        var leader = account("lider");
        createGroup(leader);
        createGroup(leader);
        var third = createGroup(leader);
        assertTrue(locked(third, leader));

        var changed = changePlan(leader, "PRO", null);

        assertEquals(200, changed.status(), changed.raw());
        assertEquals("PRO", changed.text("plan"));
        assertEquals(3, changed.json().get("availableSlots").asInt(), "5 permitidos - 2 activos");
        assertEquals(1, changed.json().get("lockedLedGroups").asInt());
        assertTrue(locked(third, leader), "el líder lo activa a mano");
        assertEquals(
                200,
                post("/groups/" + third + "/activate", leader.token(), null).status());
        assertFalse(locked(third, leader));
        assertEquals("PRO", get("/subscriptions/me", leader.token()).text("plan"));
    }

    @Test
    void aProLeaderCreatesFiveActiveGroupsAndTheSixthIsLocked() {
        var leader = account("lider");
        assertEquals(200, changePlan(leader, "PRO", null).status());
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 6; i++) ids.add(createGroup(leader));

        for (int i = 0; i < 5; i++) assertFalse(locked(ids.get(i), leader), "grupo " + i);
        assertTrue(locked(ids.get(5), leader));
    }

    @Test
    void anUltraLeaderNeverHitsALimit() {
        var leader = account("lider");
        assertEquals(200, changePlan(leader, "ULTRA", null).status());
        for (int i = 0; i < 7; i++) assertFalse(locked(createGroup(leader), leader));
        assertTrue(get("/subscriptions/me", leader.token())
                .json()
                .get("maxLedGroups")
                .isNull());
    }

    @Test
    void downgradingAsksWhichGroupsStayActive() {
        var leader = account("lider");
        assertEquals(200, changePlan(leader, "PRO", null).status());
        var g1 = createGroup(leader);
        var g2 = createGroup(leader);
        var g3 = createGroup(leader);

        // Sin elegir, o eligiendo mal, no cambia nada (ni el plan ni los grupos).
        assertEquals(409, changePlan(leader, "FREE", null).status());
        assertEquals(409, changePlan(leader, "FREE", List.of(g1)).status());
        assertEquals(409, changePlan(leader, "FREE", List.of(g1, g2, g3)).status());
        assertEquals(409, changePlan(leader, "FREE", List.of(g1, 999999L)).status());
        assertEquals("PRO", get("/subscriptions/me", leader.token()).text("plan"));
        assertFalse(locked(g3, leader));

        var downgraded = changePlan(leader, "FREE", List.of(g1, g3));

        assertEquals(200, downgraded.status(), downgraded.raw());
        assertEquals("FREE", downgraded.text("plan"));
        assertEquals(2, downgraded.json().get("activeLedGroups").asInt());
        assertEquals(1, downgraded.json().get("lockedLedGroups").asInt());
        assertFalse(locked(g1, leader));
        assertTrue(locked(g2, leader), "el que no eligió queda bloqueado");
        assertFalse(locked(g3, leader));
    }

    @Test
    void downgradingWhenEverythingFitsNeedsNoSelection() {
        var leader = account("lider");
        assertEquals(200, changePlan(leader, "PRO", null).status());
        createGroup(leader);

        assertEquals(200, changePlan(leader, "FREE", null).status());
    }

    @Test
    void youCannotKeepActiveAGroupOfSomeoneElse() {
        var leader = account("lider");
        var other = account("otro");
        assertEquals(200, changePlan(leader, "PRO", null).status());
        var mine1 = createGroup(leader);
        createGroup(leader);
        createGroup(leader);
        var foreign = createGroup(other);

        assertEquals(409, changePlan(leader, "FREE", List.of(mine1, foreign)).status());
        assertFalse(locked(foreign, other), "el grupo ajeno no se toca");
    }

    @Test
    void changingYourPlanDoesNotAffectOtherUsersGroups() {
        var leader = account("lider");
        var other = account("otro");
        createGroup(other);
        createGroup(other);
        var otherThird = createGroup(other);

        assertEquals(200, changePlan(leader, "ULTRA", null).status());

        assertTrue(locked(otherThird, other), "el plan de otro no libera mis grupos");
        assertEquals("FREE", get("/subscriptions/me", other.token()).text("plan"));
    }

    @Test
    void anInvalidPlanIsRejected() {
        var user = account("usuario");
        assertEquals(400, changePlan(user, "GOLD", null).status());
        assertEquals(400, put("/subscriptions/me", user.token(), Map.of()).status());
        assertEquals(
                400, put("/subscriptions/me", user.token(), Map.of("plan", " ")).status());
        assertEquals("FREE", get("/subscriptions/me", user.token()).text("plan"));
    }
}
