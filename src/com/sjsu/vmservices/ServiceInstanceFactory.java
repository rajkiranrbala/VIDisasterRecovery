package com.sjsu.vmservices;

import com.vmware.vim25.mo.ServiceInstance;

public abstract class ServiceInstanceFactory {

	public abstract ServiceInstance getServiceInstance();
}
