/**
 * 순수 WebSocket 테스트를 위한 수동 입찰 스크립트
 * 경매 19번에 입찰하여 실시간 업데이트 확인
 */

const axios = require('axios');

async function testManualBid() {
    console.log('🚀 순수 WebSocket 실시간 입찰 테스트 시작');
    
    try {
        // 경매 19번에 입찰
        const auctionId = 19;
        const bidAmount = 16500; // 현재가보다 높은 금액
        
        console.log(`📈 경매 ${auctionId}에 ${bidAmount}원 입찰 시도`);
        
        const response = await axios.post('http://localhost:8080/api/bids', {
            auctionId: auctionId,
            bidAmount: bidAmount,
            isAutoBid: false
        }, {
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        console.log('✅ 입찰 성공:', response.data);
        console.log('📡 이제 앱에서 실시간 업데이트를 확인하세요!');
        
    } catch (error) {
        if (error.response) {
            console.error('❌ 입찰 실패:', error.response.status, error.response.data);
        } else {
            console.error('❌ 네트워크 오류:', error.message);
        }
    }
}

// 5초 후 입찰 실행 (WebSocket 연결 안정화 대기)
setTimeout(() => {
    testManualBid();
}, 5000);

console.log('⏳ 5초 후 입찰을 실행합니다. 앱에서 경매 19번 상세 화면을 열어두세요!');