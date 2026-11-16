// Copyright (c) 2026 Ace360X contributors. BSD 3-Clause (see LICENSE).
#include "vulkan_probe.h"

#include <vulkan/vulkan.h>

#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

namespace ace360x::host {
namespace {

std::string JsonEscape(const char* s) {
  std::string out;
  for (const char* p = s; p && *p; ++p) {
    const unsigned char c = static_cast<unsigned char>(*p);
    switch (c) {
      case '"': out += "\\\""; break;
      case '\\': out += "\\\\"; break;
      case '\n': out += "\\n"; break;
      default:
        if (c < 0x20) {
          char buf[8];
          std::snprintf(buf, sizeof(buf), "\\u%04x", c);
          out += buf;
        } else {
          out += static_cast<char>(c);
        }
    }
  }
  return out;
}

std::string VersionString(uint32_t v) {
  return std::to_string(VK_API_VERSION_MAJOR(v)) + "." + std::to_string(VK_API_VERSION_MINOR(v)) +
         "." + std::to_string(VK_API_VERSION_PATCH(v));
}

const char* DeviceTypeName(VkPhysicalDeviceType t) {
  switch (t) {
    case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU: return "integrated";
    case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU: return "discrete";
    case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU: return "virtual";
    case VK_PHYSICAL_DEVICE_TYPE_CPU: return "cpu";
    default: return "other";
  }
}

std::string Error(const std::string& message) {
  return "{\"error\":\"" + JsonEscape(message.c_str()) + "\"}";
}

// RAII guard so the instance is always destroyed.
struct InstanceGuard {
  VkInstance instance = VK_NULL_HANDLE;
  ~InstanceGuard() {
    if (instance != VK_NULL_HANDLE) vkDestroyInstance(instance, nullptr);
  }
};

}  // namespace

std::string ProbeVulkanAsJson() {
  uint32_t loader_version = VK_API_VERSION_1_0;
  auto enumerate_version = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(
      vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion"));
  if (enumerate_version) enumerate_version(&loader_version);
  if (loader_version < VK_API_VERSION_1_1) {
    return Error("Vulkan 1.1 is required; this device's loader reports " +
                 VersionString(loader_version));
  }

  VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};
  app.pApplicationName = "Ace360X";
  app.apiVersion = VK_API_VERSION_1_1;
  VkInstanceCreateInfo ci{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
  ci.pApplicationInfo = &app;

  InstanceGuard guard;
  if (VkResult r = vkCreateInstance(&ci, nullptr, &guard.instance); r != VK_SUCCESS) {
    return Error("vkCreateInstance failed (VkResult " + std::to_string(r) + ")");
  }

  uint32_t count = 0;
  vkEnumeratePhysicalDevices(guard.instance, &count, nullptr);
  if (count == 0) return Error("No Vulkan physical devices found");
  std::vector<VkPhysicalDevice> devices(count);
  vkEnumeratePhysicalDevices(guard.instance, &count, devices.data());
  VkPhysicalDevice gpu = devices[0];

  VkPhysicalDeviceProperties props{};
  vkGetPhysicalDeviceProperties(gpu, &props);
  VkPhysicalDeviceFeatures f{};
  vkGetPhysicalDeviceFeatures(gpu, &f);

  uint32_t ext_count = 0;
  vkEnumerateDeviceExtensionProperties(gpu, nullptr, &ext_count, nullptr);
  std::vector<VkExtensionProperties> exts(ext_count);
  vkEnumerateDeviceExtensionProperties(gpu, nullptr, &ext_count, exts.data());
  auto has_ext = [&](const char* name) {
    for (const auto& e : exts) {
      if (std::strcmp(e.extensionName, name) == 0) return true;
    }
    return false;
  };

  // Driver name/version string (VK_KHR_driver_properties), if exposed.
  std::string driver_name;
  std::string driver_info;
  if (has_ext(VK_KHR_DRIVER_PROPERTIES_EXTENSION_NAME)) {
    auto get_props2 = reinterpret_cast<PFN_vkGetPhysicalDeviceProperties2>(
        vkGetInstanceProcAddr(guard.instance, "vkGetPhysicalDeviceProperties2"));
    if (get_props2) {
      VkPhysicalDeviceDriverPropertiesKHR driver{
          VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES_KHR};
      VkPhysicalDeviceProperties2 p2{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2};
      p2.pNext = &driver;
      get_props2(gpu, &p2);
      driver_name = driver.driverName;
      driver_info = driver.driverInfo;
    }
  }

  auto b = [](VkBool32 v) { return v ? "true" : "false"; };
  auto e = [&](const char* n) { return has_ext(n) ? "true" : "false"; };

  std::string j = "{";
  j += "\"deviceName\":\"" + JsonEscape(props.deviceName) + "\",";
  j += "\"deviceType\":\"" + std::string(DeviceTypeName(props.deviceType)) + "\",";
  j += "\"vendorId\":" + std::to_string(props.vendorID) + ",";
  j += "\"apiVersion\":\"" + VersionString(props.apiVersion) + "\",";
  j += "\"driverVersionRaw\":" + std::to_string(props.driverVersion) + ",";
  j += "\"driverName\":\"" + JsonEscape(driver_name.c_str()) + "\",";
  j += "\"driverInfo\":\"" + JsonEscape(driver_info.c_str()) + "\",";
  j += "\"maxImageDimension2D\":" + std::to_string(props.limits.maxImageDimension2D) + ",";
  j += "\"features\":{";
  j += std::string("\"geometryShader\":") + b(f.geometryShader) + ",";
  j += std::string("\"independentBlend\":") + b(f.independentBlend) + ",";
  j += std::string("\"fragmentStoresAndAtomics\":") + b(f.fragmentStoresAndAtomics) + ",";
  j += std::string("\"vertexPipelineStoresAndAtomics\":") + b(f.vertexPipelineStoresAndAtomics) + ",";
  j += std::string("\"samplerAnisotropy\":") + b(f.samplerAnisotropy) + ",";
  j += std::string("\"textureCompressionBC\":") + b(f.textureCompressionBC) + ",";
  j += std::string("\"fullDrawIndexUint32\":") + b(f.fullDrawIndexUint32) + ",";
  j += std::string("\"depthClamp\":") + b(f.depthClamp) + ",";
  j += std::string("\"shaderClipDistance\":") + b(f.shaderClipDistance);
  j += "},\"extensions\":{";
  j += std::string("\"VK_EXT_fragment_shader_interlock\":") + e("VK_EXT_fragment_shader_interlock") + ",";
  j += std::string("\"VK_EXT_shader_stencil_export\":") + e("VK_EXT_shader_stencil_export") + ",";
  j += std::string("\"VK_EXT_custom_border_color\":") + e("VK_EXT_custom_border_color") + ",";
  j += std::string("\"VK_KHR_driver_properties\":") + e(VK_KHR_DRIVER_PROPERTIES_EXTENSION_NAME);
  j += "},\"extensionCount\":" + std::to_string(ext_count);
  j += "}";
  return j;
}

}  // namespace ace360x::host
