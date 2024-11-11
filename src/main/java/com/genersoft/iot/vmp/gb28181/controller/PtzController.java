package com.genersoft.iot.vmp.gb28181.controller;


import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.UUID;

@Tag(name  = "前端设备控制")
@Slf4j
@RestController
@RequestMapping("/api/front-end")
public class PtzController {

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Operation(summary = "通用前端控制命令(参考国标文档A.3.1指令格式)", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cmdCode", description = "指令码(对应国标文档指令格式中的字节4)", required = true)
	@Parameter(name = "parameter1", description = "数据一(对应国标文档指令格式中的字节5)", required = true)
	@Parameter(name = "parameter2", description = "数据二(对应国标文档指令格式中的字节6)", required = true)
	@Parameter(name = "combindCode2", description = "组合码二(对应国标文档指令格式中的字节7:组合码2,高4位是数据3,低4位是地址的高4位)", required = true)
	@PostMapping("/common/{deviceId}/{channelId}")
	public void frontEndCommand(@PathVariable String deviceId,@PathVariable String channelId,int cmdCode, int parameter1, int parameter2, int combindCode2){

		if (log.isDebugEnabled()) {
			log.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，cmdCode：%d parameter1：%d parameter2：%d",deviceId, channelId, cmdCode, parameter1, parameter2));
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);

		try {
			cmder.frontEndCmd(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("[命令发送失败] 前端控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}

	@Operation(summary = "云台控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
	@Parameter(name = "horizonSpeed", description = "水平速度(0-255)", required = true)
	@Parameter(name = "verticalSpeed", description = "垂直速度(0-255)", required = true)
	@Parameter(name = "zoomSpeed", description = "缩放速度(0-16)", required = true)
	@PostMapping("/ptz/{deviceId}/{channelId}")
	public void ptz(@PathVariable String deviceId,@PathVariable String channelId, String command, int horizonSpeed, int verticalSpeed, int zoomSpeed){

		if (log.isDebugEnabled()) {
			log.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，command：%s ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed));
		}

		int cmdCode = 0;
		switch (command){
			case "left":
				cmdCode = 2;
				break;
			case "right":
				cmdCode = 1;
				break;
			case "up":
				cmdCode = 8;
				break;
			case "down":
				cmdCode = 4;
				break;
			case "upleft":
				cmdCode = 10;
				break;
			case "upright":
				cmdCode = 9;
				break;
			case "downleft":
				cmdCode = 6;
				break;
			case "downright":
				cmdCode = 5;
				break;
			case "zoomin":
				cmdCode = 16;
				break;
			case "zoomout":
				cmdCode = 32;
				break;
			case "stop":
				horizonSpeed = 0;
				verticalSpeed = 0;
				zoomSpeed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
	}


	@Operation(summary = "光圈控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: in, out, stop", required = true)
	@Parameter(name = "speed", description = "光圈速度(0-255)", required = true)
	@PostMapping("/fi/iris/{deviceId}/{channelId}")
	public void iris(@PathVariable String deviceId,@PathVariable String channelId, String command, int speed){

		if (log.isDebugEnabled()) {
			log.debug("设备光圈控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceId, channelId, command, speed);
		}

		int cmdCode = 0x40;
		switch (command){
			case "in":
				cmdCode = 0x48;
				break;
			case "out":
				cmdCode = 0x44;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, 0, speed, 0);
	}

	@Operation(summary = "聚焦控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: near, far, stop", required = true)
	@Parameter(name = "speed", description = "聚焦速度(0-255)", required = true)
	@PostMapping("/fi/focus/{deviceId}/{channelId}")
	public void focus(@PathVariable String deviceId,@PathVariable String channelId, String command, int speed){

		if (log.isDebugEnabled()) {
			log.debug("设备聚焦控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceId, channelId, command, speed);
		}

		int cmdCode = 0x40;
		switch (command){
			case "near":
				cmdCode = 0x42;
				break;
			case "far":
				cmdCode = 0x41;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, speed, 0, 0);
	}

	@Operation(summary = "查询预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/preset/query/{deviceId}/{channelId}")
	public DeferredResult<String> queryPreset(@PathVariable String deviceId, @PathVariable String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("设备预置位查询API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		String uuid =  UUID.randomUUID().toString();
		String key =  DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + (ObjectUtils.isEmpty(channelId) ? deviceId : channelId);
		DeferredResult<String> result = new DeferredResult<String> (3 * 1000L);
		result.onTimeout(()->{
			log.warn(String.format("获取设备预置位超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("获取设备预置位超时");
			resultHolder.invokeResult(msg);
		});
		if (resultHolder.exist(key, null)) {
			return result;
		}
		resultHolder.put(key, uuid, result);
		try {
			cmder.presetQuery(device, channelId, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("获取设备预置位失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			});
		} catch (InvalidArgumentException | SipException | ParseException e) {
			log.error("[命令发送失败] 获取设备预置位: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
		return result;
	}

	@Operation(summary = "预置位指令-设置预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "presetId", description = "预置位编号(0-255)", required = true)
	@GetMapping("/preset/add/{deviceId}/{channelId}")
	public void addPreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x81, 1, presetId, 0);
	}

	@Operation(summary = "预置位指令-调用预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "presetId", description = "预置位编号(0-255)", required = true)
	@GetMapping("/preset/call/{deviceId}/{channelId}")
	public void callPreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x82, 1, presetId, 0);
	}

	@Operation(summary = "预置位指令-删除预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "presetId", description = "预置位编号(0-255)", required = true)
	@GetMapping("/preset/delete/{deviceId}/{channelId}")
	public void deletePreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x83, 1, presetId, 0);
	}

	@Operation(summary = "巡航指令-加入巡航点", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号(0-255)", required = true)
	@Parameter(name = "presetId", description = "预置位编号(0-255)", required = true)
	@GetMapping("/cruise/point/add/{deviceId}/{channelId}")
	public void addCruisePoint(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer presetId) {
		if (presetId == null || cruiseId == null || presetId < 1 || presetId > 255 || cruiseId < 1 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x84, cruiseId, presetId, 0);
	}

	@Operation(summary = "巡航指令-删除一个巡航点", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号(1-255)", required = true)
	@Parameter(name = "presetId", description = "预置位编号(0-255, 为0时删除整个巡航)", required = true)
	@GetMapping("/cruise/point/delete/{deviceId}/{channelId}")
	public void deleteCruisePoint(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer presetId) {
		if (presetId == null || cruiseId == null || presetId < 0 || presetId > 255 || cruiseId < 1 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x85, cruiseId, presetId, 0);
	}

	@Operation(summary = "巡航指令-设置巡航速度", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号(0-255)", required = true)
	@Parameter(name = "speed", description = "巡航速度(1-4095)", required = true)
	@GetMapping("/cruise/speed/{deviceId}/{channelId}")
	public void setCruiseSpeed(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer speed) {
		if (cruiseId == null || cruiseId < 1 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
		}
		if (speed == null || speed < 1 || speed > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航速度必须为1-4095之间的数字");
		}
		int parameter2 = speed >> 4;
		int combindCode2 =  speed - parameter2 << 4 ;
		frontEndCommand(deviceId, channelId, 0x86, cruiseId, parameter2, combindCode2);
	}

	@Operation(summary = "巡航指令-设置巡航停留时间", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号", required = true)
	@Parameter(name = "time", description = "巡航停留时间(1-4095)", required = true)
	@GetMapping("/cruise/time/{deviceId}/{channelId}")
	public void setCruiseTime(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer time) {
		if (cruiseId == null || cruiseId < 1 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
		}
		if (time == null || time < 1 || time > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航停留时间必须为1-4095之间的数字");
		}
		int parameter2 = time >> 4;
		int combindCode2 =  time - parameter2 << 4 ;
		frontEndCommand(deviceId, channelId, 0x87, cruiseId, parameter2, combindCode2);
	}

	@Operation(summary = "巡航指令-开始巡航", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号)", required = true)
	@GetMapping("/cruise/start/{deviceId}/{channelId}")
	public void startCruise(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId) {
		if (cruiseId == null || cruiseId < 1 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x88, cruiseId, 0, 0);
	}

	@Operation(summary = "巡航指令-停止巡航", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号", required = true)
	@GetMapping("/cruise/stop/{deviceId}/{channelId}")
	public void stopCruise(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId) {
		if (cruiseId == null || cruiseId < 1 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0, 0, 0, 0);
	}

	@Operation(summary = "扫描指令-开始自动扫描", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/start/{deviceId}/{channelId}")
	public void startScan(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 1 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 0, 0);
	}

	@Operation(summary = "扫描指令-停止自动扫描", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/stop/{deviceId}/{channelId}")
	public void stopScan(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 1 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0, 0, 0, 0);
	}

	@Operation(summary = "扫描指令-设置自动扫描左边界", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/set/left/{deviceId}/{channelId}")
	public void setScanLeft(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 1 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 1, 0);
	}

	@Operation(summary = "扫描指令-设置自动扫描右边界", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/set/right/{deviceId}/{channelId}")
	public void setScanRight(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 1 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 2, 0);
	}


	@Operation(summary = "扫描指令-设置自动扫描速度", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@Parameter(name = "speed", description = "自动扫描速度(1-16)", required = true)
	@GetMapping("/scan/set/speed/{deviceId}/{channelId}")
	public void setScanSpeed(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId, Integer speed) {
		if (scanId == null || scanId < 1 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为1-255之间的数字");
		}
		if (speed == null || speed < 1 || speed > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "自动扫描速度必须为1-4095之间的数字");
		}
		int parameter2 = speed >> 4;
		int combindCode2 =  speed - parameter2 << 4 ;
		frontEndCommand(deviceId, channelId, 0x8A, scanId, parameter2, combindCode2);
	}


	@Operation(summary = "辅助开关控制指令-雨刷控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: on, off", required = true)
	@PostMapping("/wiper/{deviceId}/{channelId}")
	public void wiper(@PathVariable String deviceId,@PathVariable String channelId, String command){

		if (log.isDebugEnabled()) {
			log.debug("辅助开关控制指令-雨刷控制 API调用，deviceId：{} ，channelId：{} ，command：{}",deviceId, channelId, command);
		}

		int cmdCode = 0;
		switch (command){
			case "on":
				cmdCode = 0x8c;
				break;
			case "off":
				cmdCode = 0x8d;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, 1, 0, 0);
	}

	@Operation(summary = "辅助开关控制指令", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: on, off", required = true)
	@Parameter(name = "switchId", description = "开关编号", required = true)
	@PostMapping("/auxiliary/{deviceId}/{channelId}")
	public void auxiliarySwitch(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer switchId){

		if (log.isDebugEnabled()) {
			log.debug("辅助开关控制指令-雨刷控制 API调用，deviceId：{} ，channelId：{} ，command：{}, switchId: {}",deviceId, channelId, command, switchId);
		}

		int cmdCode = 0;
		switch (command){
			case "on":
				cmdCode = 0x8c;
				break;
			case "off":
				cmdCode = 0x8d;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, switchId, 0, 0);
	}
}
