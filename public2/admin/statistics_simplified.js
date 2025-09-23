/**
 * 간소화된 통계 대시보드 JavaScript
 * 5개 핵심 통계 항목만 포함
 */

$(document).ready(function() {
    let token = sessionStorage.getItem("token");
    let autoRefreshInterval;

    // 페이지 로드 시 초기화
    initializeYearOptions();
    initializeMonthOptions();
    initializePartnerOptions();
    initializeDashboard();

    /**
     * 년도 선택 옵션 초기화 (2025년부터 현재 년도까지)
     */
    function initializeYearOptions() {
        const yearSelect = $('#yearSelect');
        const now = new Date();
        const currentYear = now.getFullYear();
        const startYear = 2025; // 시작 년도

        // 기존 옵션 제거
        yearSelect.empty();

        // 2025년부터 현재 년도까지 옵션 생성
        for (let year = startYear; year <= currentYear; year++) {
            const isSelected = (year === currentYear) ? 'selected' : '';
            yearSelect.append(`<option value="${year}" ${isSelected}>${year}년</option>`);
        }
    }

    /**
     * 제휴사 선택 옵션 초기화 (사용자 그룹에 따른 제한)
     */
    function initializePartnerOptions() {
        const partnerSelect = $('#partnerSelect');
        const userGroup = sessionStorage.getItem("group");
        const username = sessionStorage.getItem("username");

        // 기존 옵션들을 모두 제거
        partnerSelect.empty();

        if (userGroup === 'admin') {
            // admin 그룹: 모든 제휴사 선택 가능
            partnerSelect.append('<option value="all">전체</option>');
            partnerSelect.append('<option value="allone">올원뱅크</option>');
            partnerSelect.val('all'); // 기본값: 전체
        } else if (userGroup === 'partner') {
            // partner 그룹: username과 동일한 제휴사만 선택 가능
            if (username === 'allone') {
                partnerSelect.append('<option value="allone">올원뱅크</option>');
                partnerSelect.val('allone');
            } else {
                // 기타 partner 사용자의 경우 기본적으로 제한
                console.warn('Unknown partner username:', username);
                partnerSelect.append('<option value="">접근 권한이 없습니다</option>');
            }

            // partner 그룹은 select 비활성화
            partnerSelect.prop('disabled', true);
        } else {
            // 알 수 없는 그룹의 경우 접근 제한
            console.warn('Unknown user group:', userGroup);
            partnerSelect.append('<option value="">접근 권한이 없습니다</option>');
            partnerSelect.prop('disabled', true);
        }

        console.log('Partner options initialized for group:', userGroup, 'username:', username);
    }

    /**
     * 월 선택 옵션 초기화 (현재 월까지만 표시)
     */
    function initializeMonthOptions() {
        const monthSelect = $('#monthSelect');
        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth() + 1; // getMonth()는 0부터 시작
        const selectedYear = parseInt($('#yearSelect').val());

        // 기존 옵션 제거
        monthSelect.empty();

        // 월 이름 배열
        const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월',
                           '7월', '8월', '9월', '10월', '11월', '12월'];

        // 선택된 년도가 현재 년도인 경우 현재 월까지만, 과거 년도면 전체 월 표시
        const maxMonth = (selectedYear === currentYear) ? currentMonth : 12;

        for (let month = 1; month <= maxMonth; month++) {
            const monthValue = month.toString().padStart(2, '0');
            const isSelected = (selectedYear === currentYear && month === currentMonth) ? 'selected' : '';
            monthSelect.append(`<option value="${monthValue}" ${isSelected}>${monthNames[month-1]}</option>`);
        }
    }

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
        // 조회 버튼 클릭 이벤트
        $('#searchBtn').on('click', function() {
            loadDashboardData();
        });

        // 새로고침 버튼 클릭 이벤트
        $('#refreshBtn').on('click', function() {
            $(this).find('i').addClass('fa-spin');
            loadDashboardData();
            setTimeout(() => {
                $(this).find('i').removeClass('fa-spin');
            }, 1000);
        });

        // 엑셀 내보내기 버튼 클릭 이벤트
        $('#excelExportBtn').on('click', function() {
            exportToExcel();
        });

        // 제휴사 선택 변경 이벤트
        $('#partnerSelect').on('change', function() {
            loadDashboardData();
        });

        // 년도 선택 변경 이벤트
        $('#yearSelect').on('change', function() {
            initializeMonthOptions(); // 년도 변경 시 월 옵션 재생성
            loadDashboardData();
        });

        // 월 선택 변경 이벤트
        $('#monthSelect').on('change', function() {
            loadDashboardData();
        });
    }

    /**
     * 대시보드 데이터 로딩
     */
    function loadDashboardData() {
        const searchParams = getSearchParams();

        // 로딩 상태 표시
        showLoadingState();

        // 월별 통계와 일별 상세 병렬 로딩
        Promise.all([
            loadMonthlyStatistics(searchParams),
            loadDailyDetails(searchParams)
        ]).then(() => {
            updateLastUpdateTime();
            updateSelectedMonthTitle(searchParams);
            hideLoadingState();
        }).catch((error) => {
            console.error('대시보드 데이터 로딩 실패:', error);
            hideLoadingState();
            // 개별 에러 처리는 각 함수에서 이미 수행됨
        });
    }

    /**
     * 검색 파라미터 생성
     */
    function getSearchParams() {
        const partner = $('#partnerSelect').val();
        const year = $('#yearSelect').val();
        const month = $('#monthSelect').val();

        return {
            partner: partner,
            year: year,
            month: month
        };
    }

    /**
     * 월별 통계 데이터 로딩 (상단 카드)
     */
    function loadMonthlyStatistics(searchParams) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/api/v1.0/admin/statistics/monthly',
                type: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                data: searchParams,
                dataType: 'json',
                success: function(response) {
                    console.log('API 응답:', response);
                    updateStatisticsCards(response);
                    resolve(response);
                },
                error: function(xhr, status, error) {
                    console.error("월별 통계 데이터 로딩 실패:", status, error, xhr.responseText);

                    // 구체적인 에러 메시지 표시
                    if (xhr.status === 401) {
                        showErrorMessage('인증이 필요합니다. 다시 로그인해주세요.');
                    } else if (xhr.status === 404) {
                        showErrorMessage('통계 데이터를 찾을 수 없습니다.');
                    } else if (xhr.status === 500) {
                        showErrorMessage('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
                    } else {
                        showErrorMessage('데이터를 불러오는 중 오류가 발생했습니다.');
                    }

                    // 에러 시에도 빈 카드 표시
                    updateStatisticsCards(getEmptyData());
                    reject(error);
                },
                complete: function(xhr) {
                    updateToken(xhr);
                }
            });
        });
    }

    /**
     * 일별 상세 데이터 로딩 (하단 테이블)
     */
    function loadDailyDetails(searchParams) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: '/api/v1.0/admin/statistics/daily',
                type: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                data: searchParams,
                dataType: 'json',
                success: function(response) {
                    console.log('일별 API 응답:', response);
                    updateDetailTable(response);
                    resolve(response);
                },
                error: function(xhr, status, error) {
                    console.error("일별 상세 데이터 로딩 실패:", status, error, xhr.responseText);

                    // 구체적인 에러 메시지 표시
                    if (xhr.status === 401) {
                        showErrorMessage('인증이 필요합니다. 다시 로그인해주세요.');
                    } else if (xhr.status === 404) {
                        showErrorMessage('일별 상세 데이터를 찾을 수 없습니다.');
                    } else if (xhr.status === 500) {
                        showErrorMessage('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
                    } else {
                        showErrorMessage('일별 상세 데이터를 불러오는 중 오류가 발생했습니다.');
                    }

                    // 에러 시 빈 테이블 표시
                    updateDetailTable({ data: { dailyDetails: [] } });
                    reject(error);
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
    function updateStatisticsCards(apiResponse) {
        if (!apiResponse) return;

        // API 응답에서 실제 데이터 추출
        const data = apiResponse.data ? apiResponse.data.monthlyStatistics : apiResponse;

        if (!data) {
            console.warn('통계 데이터가 없습니다:', apiResponse);
            return;
        }

        // 각 통계 카드에 데이터 바인딩
        $('#newSubscribers').text(formatNumber(data.newSubscribers || 0));
        $('#monthlyCancellations').text(formatNumber(data.monthlyCancellations || 0));
        $('#totalSubscribers').text(formatNumber(data.totalSubscribers || 0));
        $('#avgUsageDays').text(formatNumber(data.usageDays || 0));  // HTML ID와 일치
        $('#settlementAmount').text(formatCurrency(data.settlementAmount || 0));

        // 변화율 업데이트
        updateChangeIndicator('#newSubscribersChange', data.newSubscribersChange);
        updateChangeIndicator('#monthlyCancellationsChange', data.monthlyCancellationsChange);
        updateChangeIndicator('#totalSubscribersChange', data.totalSubscribersChange);
    }

    /**
     * 상세 테이블 업데이트
     */
    function updateDetailTable(apiResponse) {
        const tbody = $('#detailTableBody');
        tbody.empty();

        // API 응답에서 실제 데이터 추출
        const data = apiResponse.data ? apiResponse.data.dailyDetails : apiResponse;

        if (!data || !Array.isArray(data) || data.length === 0) {
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
                    <td>${formatDay(row.date)}</td>
                    <td class="text-success">${formatNumber(row.dailyNewSubscribers || 0)}</td>
                    <td class="text-danger">${formatNumber(row.dailyCancellations || 0)}</td>
                    <td class="text-info">${formatNumber(row.totalSubscribers || 0)}</td>
                    <td class="text-warning">${formatNumber(row.dailyUsageDays || 0)}일</td>
                </tr>
            `);
        });
    }

    /**
     * 변화율 표시 업데이트
     */
    function updateChangeIndicator(selector, change) {
        const element = $(selector);
        if (!change) return;

        // NULL 값 처리 (0에서 증가하는 경우)
        if (change.value === null || change.value === undefined) {
            element.removeClass('text-success text-danger').addClass('text-primary');
            element.find('i').removeClass('bi-arrow-up bi-arrow-down').addClass('bi-plus-circle');
            element.find('span').text(`신규 (${change.period})`);
            return;
        }

        const isPositive = change.value >= 0;
        const icon = isPositive ? 'bi-arrow-up' : 'bi-arrow-down';
        const colorClass = isPositive ? 'text-success' : 'text-danger';

        element.removeClass('text-success text-danger text-primary').addClass(colorClass);
        element.find('i').removeClass('bi-arrow-up bi-arrow-down bi-plus-circle').addClass(icon);
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
        console.error(message);

        // 기존 알림 제거
        $('.error-alert').remove();

        // Bootstrap Alert으로 오류 메시지 표시
        const alertHtml = `
            <div class="alert alert-danger alert-dismissible fade show error-alert" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;

        $('.dashboard-header').after(alertHtml);

        // 5초 후 자동 제거
        setTimeout(() => {
            $('.error-alert').fadeOut(() => $('.error-alert').remove());
        }, 5000);
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
     * 일자 포맷팅 (년/월/일)
     */
    function formatDay(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    }

    /**
     * 선택된 월 제목 업데이트
     */
    function updateSelectedMonthTitle(searchParams) {
        const year = searchParams.year;
        const month = searchParams.month;

        if (month === 'all') {
            $('#selectedMonthTitle').text(`${year}년 전체`);
        } else {
            $('#selectedMonthTitle').text(`${year}년 ${parseInt(month)}월`);
        }
    }

    /**
     * 샘플 월별 통계 데이터 (상단 카드용)
     */
    function getSampleMonthlyData() {
        return {
            newSubscribers: 1250,        // 월 신규가입자
            monthlyCancellations: 180,   // 월 해지자
            totalSubscribers: 12400,     // 누적가입자
            usageDays: 45.3,          // 월 사용일수
            settlementAmount: 18500000,  // 월 정산 금액
            newSubscribersChange: { value: 12.5, period: '전월 대비' },
            monthlyCancellationsChange: { value: -8.2, period: '전월 대비' },
            totalSubscribersChange: { value: 15.3, period: '전월 대비' }
        };
    }

    /**
     * 빈 데이터 (에러 시 표시용)
     */
    function getEmptyData() {
        return {
            newSubscribers: 0,
            monthlyCancellations: 0,
            totalSubscribers: 0,
            usageDays: 0,
            settlementAmount: 0,
            newSubscribersChange: { value: 0, period: '전월 대비' },
            monthlyCancellationsChange: { value: 0, period: '전월 대비' },
            totalSubscribersChange: { value: 0, period: '전월 대비' }
        };
    }

    /**
     * 샘플 일별 상세 데이터 (하단 테이블용)
     */
    function getSampleDailyData() {
        const data = [];
        const today = new Date();
        const year = today.getFullYear();
        const month = today.getMonth();

        // 해당 월의 모든 일자 생성
        const daysInMonth = new Date(year, month + 1, 0).getDate();

        for (let day = 1; day <= daysInMonth; day++) {
            const date = new Date(year, month, day);

            data.push({
                date: date.toISOString().split('T')[0],
                dailyNewSubscribers: Math.floor(Math.random() * 80) + 20,    // 일 신규가입자 (20-100)
                dailyCancellations: Math.floor(Math.random() * 20) + 5,      // 일 해지자 (5-25)
                totalSubscribers: 12400 + day * 10,                          // 누적가입자 (점진적 증가)
                dailyUsageDays: Math.floor(Math.random() * 20) + 40,      // 일 사용일수 (40-60)
                dailySettlementAmount: Math.floor(Math.random() * 500000) + 300000  // 일 정산 금액 (30-80만원)
            });
        }

        return data;
    }

    /**
     * 로딩 상태 표시
     */
    function showLoadingState() {
        $('.stat-number').text('로딩중...');
        $('.stat-change span').text('--');
    }

    /**
     * 로딩 상태 숨김
     */
    function hideLoadingState() {
        // 로딩 상태는 데이터 업데이트 시 자동으로 해제됨
    }

    /**
     * 엑셀 내보내기
     */
    function exportToExcel() {
        const partner = $('#partnerSelect').val();
        const year = $('#yearSelect').val();
        const month = $('#monthSelect').val();

        // 로딩 상태 표시
        const exportBtn = $('#excelExportBtn');
        const originalText = exportBtn.html();
        exportBtn.prop('disabled', true).html('<i class="bi bi-hourglass-split"></i> 내보내는 중...');

        // API 호출
        $.ajax({
            url: '/api/v1.0/admin/statistics/export',
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            data: {
                partner: partner,
                year: year,
                month: month
            },
            success: function(response) {
                try {
                    if (response.success && response.data && response.data.userList) {
                        generateExcelFile(response.data, partner, year, month);
                        showToast('success', '엑셀 파일이 성공적으로 다운로드되었습니다.');
                    } else {
                        throw new Error('서버에서 데이터를 가져오는데 실패했습니다.');
                    }
                } catch (error) {
                    console.error('Excel generation error:', error);
                    showToast('error', '엑셀 파일 생성 중 오류가 발생했습니다: ' + error.message);
                }
            },
            error: function(xhr, status, error) {
                console.error('Export API error:', error);
                let errorMessage = '데이터를 불러오는데 실패했습니다.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                }
                showToast('error', errorMessage);
            },
            complete: function() {
                // 로딩 상태 해제
                exportBtn.prop('disabled', false).html(originalText);
            }
        });
    }

    /**
     * 엑셀 파일 생성 및 다운로드
     */
    function generateExcelFile(data, partner, year, month) {
        try {
            // 워크북 생성
            const workbook = XLSX.utils.book_new();

            // 헤더 정의
            const headers = [
                'No',
                '통신사',
                '전화번호',
                '가입상태',
                '가입구분',
                '가입일',
                '해지일',
                '사용일수'
            ];

            // 데이터 변환
            const excelData = [headers];
            data.userList.forEach(user => {
                excelData.push([
                    user.no || '',
                    getSpcodeName(user.spcode) || '',
                    formatPhoneNumber(user.mobileno) || '',
                    getStatusName(user.status) || '',
                    getPartnerName(user.offercode) || '',
                    user.created_date || '',
                    user.canceled_date || '',
                    user.usage_days || 0
                ]);
            });

            // 워크시트 생성
            const worksheet = XLSX.utils.aoa_to_sheet(excelData);

            // 컬럼 너비 설정
            const columnWidths = [
                { wch: 8 },   // No
                { wch: 12 },  // 통신사
                { wch: 15 },  // 전화번호
                { wch: 10 },  // 가입상태
                { wch: 12 },  // 가입구분
                { wch: 12 },  // 가입일
                { wch: 12 },  // 해지일
                { wch: 10 }   // 사용일수
            ];
            worksheet['!cols'] = columnWidths;

            // 헤더 스타일 설정
            const headerStyle = {
                font: { bold: true },
                fill: { fgColor: { rgb: "E0E0E0" } },
                alignment: { horizontal: "center", vertical: "center" }
            };

            // 헤더에 스타일 적용
            for (let i = 0; i < headers.length; i++) {
                const cellRef = XLSX.utils.encode_cell({ r: 0, c: i });
                if (!worksheet[cellRef]) worksheet[cellRef] = {};
                worksheet[cellRef].s = headerStyle;
            }

            // 워크북에 워크시트 추가
            const sheetName = `${getPartnerDisplayName(partner)}_${year}년${month}월_가입자목록`;
            XLSX.utils.book_append_sheet(workbook, worksheet, sheetName);

            // 파일명 생성
            const fileName = `${getPartnerDisplayName(partner)}_${year}년${month}월_가입자목록_${getCurrentTimestamp()}.xlsx`;

            // 파일 다운로드
            XLSX.writeFile(workbook, fileName);

        } catch (error) {
            console.error('Excel file generation error:', error);
            throw new Error('엑셀 파일 생성 중 오류가 발생했습니다.');
        }
    }

    /**
     * 통신사 코드를 이름으로 변환
     */
    function getSpcodeName(spcode) {
        const spcodeMap = {
            'SKT': 'SKT',
            'KT': 'KT',
            'LGU': 'LG U+',
            'SKM': 'SKT',
            'KTM': 'KT',
            'LGM': 'LG U+'
        };
        return spcodeMap[spcode] || spcode || '';
    }

    /**
     * 상태 코드를 이름으로 변환
     */
    function getStatusName(status) {
        const statusMap = {
            'A': '정상',
            'D': '해지'
        };
        return statusMap[status] || status || '';
    }

    /**
     * 제휴사 코드를 이름으로 변환
     */
    function getPartnerName(offercode) {
        const partnerMap = {
            'allone': '올원뱅크',
            '12': '버즈빌',
            '00': '홈페이지',
            '11': 'OHC',
            '91': 'ARS'
        };
        return partnerMap[offercode] || offercode || '';
    }

    /**
     * 제휴사 표시명 가져오기
     */
    function getPartnerDisplayName(partner) {
        if (partner === 'all') {
            return '전체';
        }
        return getPartnerName(partner);
    }

    /**
     * 전화번호 포맷팅
     */
    function formatPhoneNumber(phoneNumber) {
        if (!phoneNumber) return '';

        // 숫자만 추출
        const numbers = phoneNumber.replace(/[^0-9]/g, '');

        // 010-XXXX-XXXX 형태로 포맷팅
        if (numbers.length === 11 && numbers.startsWith('010')) {
            return numbers.replace(/(\d{3})(\d{4})(\d{4})/, '$1$2$3');
        }

        return phoneNumber; // 원본 그대로 반환
    }

    /**
     * 현재 타임스탬프 생성
     */
    function getCurrentTimestamp() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');

        return `${year}${month}${day}_${hours}${minutes}${seconds}`;
    }

    /**
     * 토스트 메시지 표시
     */
    function showToast(type, message) {
        // Bootstrap Toast를 사용하거나 간단한 alert 사용
        if (type === 'success') {
            // 성공 메시지는 콘솔에만 출력 (다운로드 완료는 브라우저에서 자동으로 알림)
            console.log('Success:', message);
        } else {
            alert(message);
        }
    }

    // 페이지 언로드 시 자동 새로고침 중지
    $(window).on('beforeunload', function() {
        stopAutoRefresh();
    });
});