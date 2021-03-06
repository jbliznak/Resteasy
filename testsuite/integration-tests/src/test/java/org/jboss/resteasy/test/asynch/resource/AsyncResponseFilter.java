package org.jboss.resteasy.test.asynch.resource;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerResponseContext;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public abstract class AsyncResponseFilter implements ContainerResponseFilter {

   private String name;
   private String callbackException;
   
   public AsyncResponseFilter(String name)
   {
      this.name = name;
   }

   @Override
   public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
         throws IOException
   {
      // copy request filter callback values
      for (Entry<String, List<String>> entry : requestContext.getHeaders().entrySet())
      {
         if(entry.getKey().startsWith("RequestFilterCallback"))
            // cast required to disambiguate with Object... method
            responseContext.getHeaders().addAll(entry.getKey(), (List)entry.getValue());
      }
      responseContext.getHeaders().add("ResponseFilterCallback"+name, String.valueOf(callbackException));
      callbackException = null;

      SuspendableContainerResponseContext ctx = (SuspendableContainerResponseContext) responseContext;
      String action = requestContext.getHeaderString(name);
      System.err.println("Filter response for "+name+" with action: "+action);
      if("sync-pass".equals(action)) {
         // do nothing
      }else if("sync-fail".equals(action)) {
         ctx.setEntity(name);
      }else if("async-pass".equals(action)) {
         ctx.suspend();
         ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.submit(() -> ctx.resume());
      }else if("async-pass-instant".equals(action)) {
         ctx.suspend();
         ctx.resume();
      }else if("async-fail".equals(action)) {
         ctx.suspend();
         ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.submit(() -> {
            ctx.setEntity(name);
            ctx.resume();
         });
      }else if("async-fail-late".equals(action)) {
         ctx.suspend();
         ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.submit(() -> {
            try
            {
               Thread.sleep(2000);
            } catch (InterruptedException e)
            {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
            ctx.setEntity(name);
            ctx.resume();
         });
      }else if("async-fail-instant".equals(action)) {
         ctx.suspend();
         ctx.setEntity(name);
         ctx.resume();
      }else if("sync-throw".equals(action)) {
         throw new AsyncFilterException("ouch");
      }else if("async-throw-late".equals(action)) {
         ctx.suspend();
         HttpRequest req = ResteasyProviderFactory.getContextData(HttpRequest.class);
         ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.submit(() -> {
            try
            {
               Thread.sleep(2000);
            } catch (InterruptedException e)
            {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
            ctx.setEntity(name);
            ResteasyAsynchronousResponse resp = req.getAsyncContext().getAsyncResponse();
            resp.register((CompletionCallback) (t) -> {
               if(callbackException != null)
                  throw new RuntimeException("Callback called twice");
               callbackException = Objects.toString(t);
            });
            if("true".equals(req.getHttpHeaders().getHeaderString("UseExceptionMapper")))
               ctx.resume(new AsyncFilterException("ouch"));
            else
               ctx.resume(new Throwable("ouch"));
         });
      }
      System.err.println("Filter response for "+name+" with action: "+action+" done");
   }
}
