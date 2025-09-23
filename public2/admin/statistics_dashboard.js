/**
 * 통계 대시보드 JavaScript
 * API 호출을 통한 데이터 로딩 및 화면 업데이트
 */

$(document).ready(function() {
    let token = sessionStorage.getItem("token");
    let currentDateRange = 'month';
    let autoRefreshInterval;

    // 페이지 로드 시 초기화
    initializeDashboard();

    /**
     * 대시보드 초기화
     */
    function initializeDashboard() {
        setupEventListeners();
        loadDashboardData();
        updateLastUpdateTime();
        startAutoRefresh();
    }

    /**
     * 이벤트 리스너 설정
     */
    function setupEventListeners() {
        // 날짜 범위 변경 이벤트
        $('#dateRange').on('change', function() {
            currentDateRange = $(this).val();
            toggleCustomDateInputs();
            loadDashboardData();
        });

        // 사용자 지정 날짜 변경 이벤트
        $('#startDate, #endDate').on('change', function() {
            if (currentDateRange === 'custom') {
                loadDashboardData();
            }
        });

        // 새로고침 버튼 클릭 이벤트
        $('#refreshBtn').on('click', function() {
            $(this).find('i').addClass('fa-spin');
            loadDashboardData();
            setTimeout(() => {
                $(this).find('i').removeClass('fa-spin');
            }, 1000);
        });
    }

    /**
     * 사용자 지정 날짜 입력 필드 토글
     */
    function toggleCustomDateInputs() {
        if (currentDateRange === 'custom') {
            $('#startDate, #endDate, #dateSeparator').show();
            // 기본값으로 이번 달 설정
            const now = new Date();
            const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
            const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

            $('#startDate').val(formatDateForInput(firstDay));
            $('#endDate').val(formatDateForInput(lastDay));
        } else {
            $('#startDate, #endDate, #dateSeparator').hide();
        }
    }

    /**
     * 날짜를 input[type="date"] 형식으로 변환
     */
    function formatDateForInput(date) {
        return date.toISOString().split('T')[0];
    }

    /**
     * 대시보드 데이터 로딩
     */
    function loadDashboardData() {
        const dateParams = getDateParams();

        // 병렬로 모든 데이터 로딩
        Promise.all([
            loadStatisticsData(dateParams),
            loadDetailTableData(dateParams)
        ]).then(() => {
            updateLastUpdateTime();
        }).catch((error) => {
            console.error('대시보드 데이터 로딩 실패:', error);
            showErrorMessage('데이터를 불러오는 중 오류가 발생했습니다.');
        });
    }

    /**
     * 날짜 파라미터 생성
     */
    function getDateParams() {
        const now = new Date();
        let startDate, endDate;

        switch (currentDateRange) {
            case 'today':
                startDate = endDate = formatDateForInput(now);
                break;
            case 'week':
                const weekStart = new Date(now);
                weekStart.setDate(now.getDate() - now.getDay());
                startDate = formatDateForInput(weekStart);
                endDate = formatDateForInput(now);
                break;
            case 'month':
                startDate = formatDateForInput(new Date(now.getFullYear(), now.getMonth(), 1));
                endDate = formatDateForInput(new Date(now.getFullYear(), now.getMonth() + 1, 0));
                break;
            case 'year':
                startDate = formatDateForInput(new Date(now.getFullYear(), 0, 1));
                endDate = formatDateForInput(new Date(now.getFullYear(), 11, 31));
                break;
            case 'custom':
                startDate = $('#startDate').val();
                endDate = $('#endDate').val();
                break;
        }

        return { startDate, endDate, range: currentDateRange };
    }

    /**
     * 통계 데이터 로딩
     */
    function loadStatisticsData(dateParams) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/api/v2.0/admin/statistics/summary',
                type: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                data: dateParams,
                dataType: 'json',
                success: function(data) {
                    updateStatisticsCards(data);
                    resolve(data);
                },
                error: function(xhr, status, error) {
                    console.error("통계 데이터 로딩 실패:", status, error);
                    // 샘플 데이터로 대체 (API 구현 전까지)
                    updateStatisticsCards(getSampleStatisticsData());
                    resolve();
                },
                complete: function(xhr) {
                    updateToken(xhr);
                }
            });
        });
    }

    /**
     * 정산 데이터 로딩
     */
    function loadSettlementData(dateParams) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/api/v2.0/admin/statistics/settlement',
                type: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                data: dateParams,
                dataType: 'json',
                success: function(data) {
                    updateSettlementCards(data);
                    resolve(data);
                },
                error: function(xhr, status, error) {
                    console.error("정산 데이터 로딩 실패:", status, error);
                    // 샘플 데이터로 대체
                    updateSettlementCards(getSampleSettlementData());
                    resolve();
                },
                complete: function(xhr) {
                    updateToken(xhr);
                }
            });
        });
    }

    /**
     * 상세 테이블 데이터 로딩
     */
    function loadDetailTableData(dateParams) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/api/v2.0/admin/statistics/detail',
                type: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                data: dateParams,
                dataType: 'json',
                success: function(data) {
                    updateDetailTable(data);
                    resolve(data);
                },
                error: function(xhr, status, error) {
                    console.error("상세 테이블 데이터 로딩 실패:", status, error);
                    // 샘플 데이터로 대체
                    updateDetailTable(getSampleDetailData());
                    resolve();
                },
                complete: function(xhr) {
                    updateToken(xhr);
                }
            });
        });
    }

    /**
     * 차트 데이터 로딩
     */
    function loadChartData(dateParams) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/api/v2.0/admin/statistics/charts',
                type: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                data: dateParams,
                dataType: 'json',
                success: function(data) {
                    updateCharts(data);
                    resolve(data);
                },
                error: function(xhr, status, error) {
                    console.error("차트 데이터 로딩 실패:", status, error);
                    // 차트는 API 구현 후 활성화
                    updateCharts(null);
                    resolve();
                },
                complete: function(xhr) {
                    updateToken(xhr);
                }
            });
        });
    }

    /**
     * 통계 카드 업데이트
     */
    function updateStatisticsCards(data) {
        if (!data) return;

        $('#newSubscribers').text(formatNumber(data.newSubscribers || 0));
        $('#monthlyCancellations').text(formatNumber(data.monthlyCancellations || 0));
        $('#totalSubscribers').text(formatNumber(data.totalSubscribers || 0));
        $('#avgUsageDays').text(formatNumber(data.avgUsageDays || 0));
        $('#settlementAmount').text(formatCurrency(data.settlementAmount || 0));

        // 변화율 업데이트
        updateChangeIndicator('#newSubscribersChange', data.newSubscribersChange);
        updateChangeIndicator('#monthlyCancellationsChange', data.monthlyCancellationsChange);
        updateChangeIndicator('#totalSubscribersChange', data.totalSubscribersChange);
    }

    /**
     * 정산 카드 업데이트
     */
    function updateSettlementCards(data) {
        if (!data) return;

        $('#totalRevenue').text(formatCurrency(data.totalRevenue || 0));
        $('#monthlyRevenue').text(formatCurrency(data.monthlyRevenue || 0));
        $('#pendingSettlement').text(formatCurrency(data.pendingSettlement || 0));
    }

    /**
     * 상세 테이블 업데이트
     */
    function updateDetailTable(data) {
        const tbody = $('#detailTableBody');
        tbody.empty();

        if (!data || !data.length) {
            tbody.append(`
                <tr>
                    <td colspan="6" class="text-center text-muted">데이터가 없습니다.</td>
                </tr>
            `);
            return;
        }

        data.forEach(row => {
            tbody.append(`
                <tr>
                    <td>${formatDate(row.date)}</td>
                    <td class="text-success">${formatNumber(row.newSubscribers || 0)}</td>
                    <td class="text-danger">${formatNumber(row.monthlyCancellations || 0)}</td>
                    <td class="text-info">${formatNumber(row.totalSubscribers || 0)}</td>
                    <td class="text-warning">${formatNumber(row.avgUsageDays || 0)}일</td>
                    <td class="text-primary">${formatCurrency(row.settlementAmount || 0)}원</td>
                </tr>
            `);
        });
    }

    /**
     * 차트 초기화
     */
    function initializeCharts() {
        // 가입자 추이 차트 (Line Chart)
        const subscribersCtx = document.getElementById('subscribersChart').getContext('2d');
        subscribersChart = new Chart(subscribersCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: '신규가입',
                    data: [],
                    borderColor: '#28a745',
                    backgroundColor: 'rgba(40, 167, 69, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4
                }, {
                    label: '해지',
                    data: [],
                    borderColor: '#dc3545',
                    backgroundColor: 'rgba(220, 53, 69, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4
                }, {
                    label: '순증가',
                    data: [],
                    borderColor: '#007bff',
                    backgroundColor: 'rgba(0, 123, 255, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            usePointStyle: true,
                            padding: 20
                        }
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false,
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
                        titleColor: '#fff',
                        bodyColor: '#fff',
                        borderColor: '#fff',
                        borderWidth: 1
                    }
                },
                scales: {
                    x: {
                        display: true,
                        title: {
                            display: true,
                            text: '날짜'
                        },
                        grid: {
                            display: false
                        }
                    },
                    y: {
                        display: true,
                        title: {
                            display: true,
                            text: '가입자 수'
                        },
                        beginAtZero: true,
                        grid: {
                            borderDash: [5, 5]
                        }
                    }
                },
                interaction: {
                    mode: 'nearest',
                    axis: 'x',
                    intersect: false
                }
            }
        });

        // 정산 현황 차트 (Doughnut Chart)
        const settlementCtx = document.getElementById('settlementChart').getContext('2d');
        settlementChart = new Chart(settlementCtx, {
            type: 'doughnut',
            data: {
                labels: ['정산 완료', '정산 대기', '정산 예정'],
                datasets: [{
                    data: [],
                    backgroundColor: [
                        '#28a745',
                        '#ffc107',
                        '#17a2b8'
                    ],
                    borderColor: [
                        '#1e7e34',
                        '#e0a800',
                        '#138496'
                    ],
                    borderWidth: 2,
                    hoverBorderWidth: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            padding: 20,
                            generateLabels: function(chart) {
                                const data = chart.data;
                                if (data.labels.length && data.datasets.length) {
                                    return data.labels.map((label, i) => {
                                        const value = data.datasets[0].data[i];
                                        const percentage = ((value / data.datasets[0].data.reduce((a, b) => a + b, 0)) * 100).toFixed(1);
                                        return {
                                            text: `${label}: ${formatCurrency(value)}원 (${percentage}%)`,
                                            fillStyle: data.datasets[0].backgroundColor[i],
                                            strokeStyle: data.datasets[0].borderColor[i],
                                            pointStyle: 'circle'
                                        };
                                    });
                                }
                                return [];
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
                        titleColor: '#fff',
                        bodyColor: '#fff',
                        borderColor: '#fff',
                        borderWidth: 1,
                        callbacks: {
                            label: function(context) {
                                const label = context.label || '';
                                const value = context.parsed;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((value / total) * 100).toFixed(1);
                                return `${label}: ${formatCurrency(value)}원 (${percentage}%)`;
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 차트 업데이트
     */
    function updateCharts(data) {
        if (data && subscribersChart && settlementChart) {
            // 가입자 추이 차트 업데이트
            if (data.subscriberTrend) {
                updateSubscriberChart(data.subscriberTrend);
            } else {
                updateSubscriberChart(getSampleSubscriberTrendData());
            }

            // 정산 차트 업데이트
            if (data.settlementStatus) {
                updateSettlementChart(data.settlementStatus);
            } else {
                updateSettlementChart(getSampleSettlementStatusData());
            }
        } else {
            // 샘플 데이터로 차트 업데이트
            updateSubscriberChart(getSampleSubscriberTrendData());
            updateSettlementChart(getSampleSettlementStatusData());
        }
    }

    /**
     * 가입자 추이 차트 업데이트
     */
    function updateSubscriberChart(trendData) {
        if (!subscribersChart || !trendData) return;

        subscribersChart.data.labels = trendData.labels;
        subscribersChart.data.datasets[0].data = trendData.newSubscribers;
        subscribersChart.data.datasets[1].data = trendData.cancellations;
        subscribersChart.data.datasets[2].data = trendData.netIncrease;
        subscribersChart.update('active');
    }

    /**
     * 정산 차트 업데이트
     */
    function updateSettlementChart(settlementData) {
        if (!settlementChart || !settlementData) return;

        settlementChart.data.datasets[0].data = [
            settlementData.completed,
            settlementData.pending,
            settlementData.scheduled
        ];
        settlementChart.update('active');
    }

    /**
     * 변화율 표시 업데이트
     */
    function updateChangeIndicator(selector, change) {
        const element = $(selector);
        if (!change) return;

        const isPositive = change.value >= 0;
        const icon = isPositive ? 'bi-arrow-up' : 'bi-arrow-down';
        const colorClass = isPositive ? 'text-success' : 'text-danger';

        element.removeClass('text-success text-danger').addClass(colorClass);
        element.find('i').removeClass('bi-arrow-up bi-arrow-down').addClass(icon);
        element.find('span').text(`${Math.abs(change.value)}% (${change.period})`);
    }

    /**
     * 마지막 업데이트 시간 표시
     */
    function updateLastUpdateTime() {
        const now = new Date();
        const timeString = now.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
        $('#lastUpdateTime').text(timeString);
    }

    /**
     * 토큰 업데이트
     */
    function updateToken(xhr) {
        const authorizationHeader = xhr.getResponseHeader('Authorization');
        if (authorizationHeader && authorizationHeader.startsWith('Bearer ')) {
            token = authorizationHeader.substring(7);
            sessionStorage.setItem("token", token);
        }
    }

    /**
     * 자동 새로고침 시작
     */
    function startAutoRefresh() {
        // 5분마다 자동 새로고침
        autoRefreshInterval = setInterval(() => {
            loadDashboardData();
        }, 5 * 60 * 1000);
    }

    /**
     * 자동 새로고침 중지
     */
    function stopAutoRefresh() {
        if (autoRefreshInterval) {
            clearInterval(autoRefreshInterval);
        }
    }

    /**
     * 오류 메시지 표시
     */
    function showErrorMessage(message) {
        // Bootstrap Toast나 Alert 활용
        console.error(message);
        // 추후 사용자 친화적인 오류 표시 구현
    }

    /**
     * 숫자 포맷팅 (천 단위 콤마)
     */
    function formatNumber(number) {
        return new Intl.NumberFormat('ko-KR').format(number);
    }

    /**
     * 통화 포맷팅
     */
    function formatCurrency(amount) {
        return new Intl.NumberFormat('ko-KR').format(amount);
    }

    /**
     * 날짜 포맷팅
     */
    function formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            month: '2-digit',
            day: '2-digit'
        });
    }

    /**
     * 샘플 통계 데이터 (API 구현 전까지 사용)
     */
    function getSampleStatisticsData() {
        return {
            newSubscribers: 1250,
            monthlyCancellations: 180,
            totalSubscribers: 12400,
            avgUsageDays: 45.3,
            settlementAmount: 18500000,
            newSubscribersChange: { value: 12.5, period: '전월 대비' },
            monthlyCancellationsChange: { value: -8.2, period: '전월 대비' },
            totalSubscribersChange: { value: 15.3, period: '전월 대비' }
        };
    }

    /**
     * 샘플 정산 데이터
     */
    function getSampleSettlementData() {
        return {
            totalRevenue: 245000000,
            monthlyRevenue: 18500000,
            pendingSettlement: 3200000
        };
    }

    /**
     * 샘플 상세 데이터
     */
    function getSampleDetailData() {
        const data = [];
        const today = new Date();

        for (let i = 6; i >= 0; i--) {
            const date = new Date(today);
            date.setDate(today.getDate() - i);

            data.push({
                date: date.toISOString().split('T')[0],
                newSubscribers: Math.floor(Math.random() * 200) + 50,
                monthlyCancellations: Math.floor(Math.random() * 50) + 10,
                totalSubscribers: 12400 + Math.floor(Math.random() * 100),
                avgUsageDays: Math.floor(Math.random() * 60) + 30,
                settlementAmount: Math.floor(Math.random() * 1000000) + 500000
            });
        }

        return data;
    }

    /**
     * 샘플 가입자 추이 데이터
     */
    function getSampleSubscriberTrendData() {
        const labels = [];
        const newSubscribers = [];
        const cancellations = [];
        const netIncrease = [];
        const today = new Date();

        // 최근 14일 데이터 생성
        for (let i = 13; i >= 0; i--) {
            const date = new Date(today);
            date.setDate(today.getDate() - i);

            const newSubs = Math.floor(Math.random() * 150) + 80; // 80-230
            const cancels = Math.floor(Math.random() * 60) + 20; // 20-80
            const net = newSubs - cancels;

            labels.push(date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' }));
            newSubscribers.push(newSubs);
            cancellations.push(cancels);
            netIncrease.push(net);
        }

        return {
            labels,
            newSubscribers,
            cancellations,
            netIncrease
        };
    }

    /**
     * 샘플 정산 현황 데이터
     */
    function getSampleSettlementStatusData() {
        return {
            completed: 18500000,   // 정산 완료
            pending: 3200000,      // 정산 대기
            scheduled: 1800000     // 정산 예정
        };
    }

    // 페이지 언로드 시 자동 새로고침 중지
    $(window).on('beforeunload', function() {
        stopAutoRefresh();
    });
});