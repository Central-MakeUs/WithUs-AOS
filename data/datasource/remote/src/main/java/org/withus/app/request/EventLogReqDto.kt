package org.koiware.demo.request


data class ControlLogRequest(
    val controlLogInfos: List<EventLogReqDto>
)

data class EventLogReqDto(
    val serialNumber: String,
    val taskId: Long,
    val lockPointId: Long,
    val keyType: String,
    val issuanceId: String,
    val resultStatus: String,
    val resultMessage: String,
    val controlStatus: String,
    val controlAt: String,
    val controlBy: Long,
)