/*
 * Copyright 2019 Maksim Zheravin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package exchange.core2.core.orderbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.core2.core.common.CoreSymbolSpecification;
import exchange.core2.core.common.L2MarketData;
import exchange.core2.core.common.OrderAction;
import exchange.core2.core.common.OrderType;
import exchange.core2.core.common.cmd.CommandResultCode;
import exchange.core2.core.common.cmd.OrderCommand;
import exchange.core2.core.common.config.LoggingConfiguration;
import exchange.core2.core.utils.Range;
import exchange.core2.tests.util.TestConstants;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class OrderBookNaiveImplStopLossTest extends OrderBookBaseTest {

    @Override
    protected IOrderBook createNewOrderBook() {
        return new OrderBookNaiveImpl(getCoreSymbolSpec(), LoggingConfiguration.DEFAULT);
    }

    @Override
    protected CoreSymbolSpecification getCoreSymbolSpec() {
        return TestConstants.SYMBOLSPECFEE_XBT_LTC;
    }

    // @BeforeEach
    // public void before() {
    // orderBook = createNewOrderBook();
    // orderBook.validateInternalState();
    // }

    @Test
    public void shouldCreateStopLossOrderFollowdByIOC() {
        clearOrderBook();
        L2MarketData snapshot = orderBook.getL2MarketDataSnapshot(10);
        OrderCommand cmd = OrderCommand.newOrder(OrderType.GTC, 400000L, UID_1, 103, 103, 100, OrderAction.BID);
        cmd.stopLoss = Range.builder().low(46).high(55).build();
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        snapshot = orderBook.getL2MarketDataSnapshot(10);
        // assertEquals(1, snapshot.bidSize);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400001L, UID_2, 98, 100, 100, OrderAction.ASK);
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        snapshot = orderBook.getL2MarketDataSnapshot(10);
        assertEquals(103, orderBook.getLastPrice());
        assertEquals(103, snapshot.getLastPrice());
        assertEquals(1, snapshot.askSize);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400002L, UID_2, 54, 19, 100, OrderAction.ASK);
        processAndValidate(cmd, CommandResultCode.SUCCESS);
        snapshot = orderBook.getL2MarketDataSnapshot(10);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400003L, UID_2, 58, 19, 100, OrderAction.BID);
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        snapshot = orderBook.getL2MarketDataSnapshot(10);
        assertEquals(54, snapshot.getLastPrice());
        assertEquals(0, snapshot.bidSize);
        assertEquals(1, snapshot.askSize);
        assertNotNull(orderBook.getOrderById(-400000L));
        assertTrue(orderBook.getOrderById(-400000L).getStopLoss().isInRange(orderBook.getLastPrice()));
        assertEquals(46, orderBook.getOrderById(-400000L).getPrice());

        cmd = OrderCommand.newOrder(OrderType.IOC, 400004L, UID_2, orderBook.getLastPrice(), 50, 100, OrderAction.BID);
        processAndValidate(cmd, CommandResultCode.SUCCESS);
        assertEquals(46, cmd.extractEvents().get(0).price);

        snapshot = orderBook.getL2MarketDataSnapshot(1000);
        assertEquals(0, orderBook.askOrdersStream(false).count());
        assertEquals(0, orderBook.bidOrdersStream(false).count());

        long askSum = Arrays.stream(snapshot.askVolumes).sum();
        long bidSum = Arrays.stream(snapshot.bidVolumes).sum();
        assertEquals(0, askSum);
        assertEquals(0, bidSum);

        assertEquals(0, Arrays.stream(snapshot.bidOrders).sum());
        assertEquals(0, Arrays.stream(snapshot.askOrders).sum());

        assertEquals(46, snapshot.getLastPrice());
        assertEquals(46, orderBook.getLastPrice());

    }

    @Test
    public void shouldCreateStopLossOrderFollowdByGTC() {
        clearOrderBook();
        L2MarketData snapshot = orderBook.getL2MarketDataSnapshot(10);
        OrderCommand cmd = OrderCommand.newOrder(OrderType.GTC, 400000L, UID_1, 103, 103, 100, OrderAction.BID);
        cmd.stopLoss = Range.builder().low(46).high(55).build();
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        snapshot = orderBook.getL2MarketDataSnapshot(10);
        // assertEquals(1, snapshot.bidSize);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400001L, UID_2, 98, 100, 100, OrderAction.ASK);
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        snapshot = orderBook.getL2MarketDataSnapshot(10);
        assertEquals(103, orderBook.getLastPrice());
        assertEquals(103, snapshot.getLastPrice());
        assertEquals(1, snapshot.askSize);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400002L, UID_2, 54, 19, 100, OrderAction.ASK);
        processAndValidate(cmd, CommandResultCode.SUCCESS);
        snapshot = orderBook.getL2MarketDataSnapshot(10);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400003L, UID_2, 58, 19, 100, OrderAction.BID);
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        snapshot = orderBook.getL2MarketDataSnapshot(10);
        assertEquals(54, snapshot.getLastPrice());
        assertEquals(0, snapshot.bidSize);
        assertEquals(1, snapshot.askSize);
        assertNotNull(orderBook.getOrderById(-400000L));
        assertTrue(orderBook.getOrderById(-400000L).getStopLoss().isInRange(orderBook.getLastPrice()));
        assertEquals(46, orderBook.getOrderById(-400000L).getPrice());

        cmd = OrderCommand.newOrder(OrderType.GTC, 400004L, UID_2, orderBook.getLastPrice(), 50, 100, OrderAction.BID);
        processAndValidate(cmd, CommandResultCode.SUCCESS);
        assertEquals(46, cmd.extractEvents().get(0).price);

        snapshot = orderBook.getL2MarketDataSnapshot(1000);
        assertEquals(0, orderBook.askOrdersStream(false).count());
        assertEquals(0, orderBook.bidOrdersStream(false).count());

        long askSum = Arrays.stream(snapshot.askVolumes).sum();
        long bidSum = Arrays.stream(snapshot.bidVolumes).sum();
        assertEquals(0, askSum);
        assertEquals(0, bidSum);

        assertEquals(0, Arrays.stream(snapshot.bidOrders).sum());
        assertEquals(0, Arrays.stream(snapshot.askOrders).sum());

        assertEquals(46, snapshot.getLastPrice());
        assertEquals(46, orderBook.getLastPrice());

    }

    final long UID_3 = 413L;
    final long UID_4 = 414L;
    final long UID_5 = 415L;

    @Test
    public void shouldCreateMultipleOrders() {

        /*
         * bid 100 x 100 sl 90-95
         * bid 101 x 100
         * ask 110 x 50
         * ask 91 x 18
         * ask 90 x 560
         */

        // UID_1 and UID_2 will bid
        OrderCommand cmd = OrderCommand.newOrder(OrderType.GTC, 400000L, UID_1, 100, 100, 100, OrderAction.BID);
        cmd.stopLoss = Range.builder().low(46).high(110).build();
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400001L, UID_2, 101, 100, 100, OrderAction.BID);
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        cmd = OrderCommand.newOrder(OrderType.GTC, 400002L, UID_3, 110, 110, 50, OrderAction.ASK);
        processAndValidate(cmd, CommandResultCode.SUCCESS);
        cmd = OrderCommand.newOrder(OrderType.GTC, 400003L, UID_4, 91, 91, 18, OrderAction.ASK);
        processAndValidate(cmd, CommandResultCode.SUCCESS);
        cmd = OrderCommand.newOrder(OrderType.GTC, 400004L, UID_5, 90, 90, 560, OrderAction.ASK);
        processAndValidate(cmd, CommandResultCode.SUCCESS);

        // assertEquals(-1000, orderBook.getLastPrice());

    }

}